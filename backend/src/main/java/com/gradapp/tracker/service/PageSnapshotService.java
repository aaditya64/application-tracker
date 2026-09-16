package com.gradapp.tracker.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Media;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Saves a full PDF snapshot of a job posting page, using a real (headless) browser so
 * JavaScript-rendered content (most modern careers sites) is actually captured. This exists so a
 * posting is still readable for interview prep after the listing itself is taken down.
 *
 * Playwright's Java API is not safe to call from multiple threads at once, so every capture runs
 * on one dedicated background thread; captures are naturally serialized, which is fine since this
 * always runs in the background anyway.
 *
 * Several things make raw "load page, print to PDF" produce a bad result, addressed here:
 *  - page.pdf() defaults to *print* media, which uses the site's (often neglected/incomplete)
 *    print stylesheet instead of what a person actually sees - forced to 'screen' media instead.
 *  - Many SPAs only render content as it scrolls into view - handled with an auto-scroll pass
 *    (both programmatic scrollBy and real wheel events, since some sites only respond to one).
 *  - `position: fixed`/`sticky` elements (cookie banners, sticky nav) repeat on every page of a
 *    paginated PDF per the CSS spec, covering whatever is underneath on every page - stripped out,
 *    but only small chrome-sized ones, never anything covering most of the viewport (that's more
 *    likely to be the page's own layout than a nuisance overlay).
 *  - Some sites set `overflow: hidden` on <html>/<body>, which Chromium's PDF export uses to decide
 *    how much content to paginate - forced back to natural height/overflow before capture.
 *
 * Bot-detection challenge pages (Cloudflare/PerimeterX/Akamai-style "checking your browser") are a
 * different problem: some sites actively detect and block any automated browser. The settings here
 * (a normal desktop user agent, hiding the most common automation fingerprint, and giving a JS
 * challenge time to clear on its own) reduce how often that happens, but there's no guaranteed
 * bypass - a site that's determined to block automation can still do so.
 *
 * Known remaining gap: a small number of sites (confirmed on one so far) render a fixed amount of
 * content in headless Chromium regardless of scrolling, viewport, or wait time, for reasons that
 * didn't trace back to any of the causes above after a thorough investigation - likely some other
 * headless-vs-headed rendering discrepancy specific to that site's JS. The PDF still captures
 * whatever did render (never blank), just not the full page in that case.
 */
@Service
public class PageSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(PageSnapshotService.class);
    private static final int NAV_TIMEOUT_MS = 20000;
    private static final int RENDER_SETTLE_MS = 2000;
    private static final int BOT_CHECK_TIMEOUT_MS = 6000;
    private static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private static final String AUTO_SCROLL_SCRIPT = """
            async () => {
              let lastHeight = 0;
              let stableCount = 0;
              for (let i = 0; i < 60 && stableCount < 3; i++) {
                window.scrollBy(0, 600);
                await new Promise((r) => setTimeout(r, 150));
                const height = document.body.scrollHeight;
                stableCount = (height === lastHeight) ? stableCount + 1 : 0;
                lastHeight = height;
              }
              window.scrollTo(0, 0);
              await new Promise((r) => setTimeout(r, 300));
            }
            """;

    private static final String HIDE_FIXED_ELEMENTS_SCRIPT = """
            () => {
              const viewportArea = window.innerWidth * window.innerHeight;
              document.querySelectorAll('*').forEach((el) => {
                const position = window.getComputedStyle(el).position;
                if (position !== 'fixed' && position !== 'sticky') return;
                const rect = el.getBoundingClientRect();
                // Only strip small chrome (banners, toolbars, chat widgets) - never something that
                // covers most of the viewport, which is far more likely to be the page's own layout
                // (some SPAs fix-position their whole app root) than a nuisance overlay. Hiding that
                // would blank the entire page, which is worse than leaving a banner in place.
                const areaFraction = (rect.width * rect.height) / viewportArea;
                if (areaFraction < 0.35 && rect.height < window.innerHeight * 0.4) {
                  el.style.setProperty('display', 'none', 'important');
                }
              });
            }
            """;

    /**
     * Chromium's PDF export measures the <html> element's box to decide how much content to
     * paginate. Two related problems both clip content the same way, and this fixes both:
     *  - Some sites set `overflow: hidden` on <html>/<body> itself (often to block page scroll
     *    while a mobile menu/modal is open, sometimes left engaged by default).
     *  - Many "master-detail" layouts (a results list next to a scrolling detail panel - the
     *    Workday job-board template is a common example) put the real content inside a nested
     *    container with its own constrained height and internal scrollbar, independent of the
     *    page root. The root looks fine; the panel silently truncates everything past its own
     *    scroll boundary.
     * This walks every element and un-clips any whose visible box is smaller than its actual
     * content (scrollHeight > clientHeight) and whose overflow would hide the rest - which by
     * construction only ever reveals more content, never hides or removes anything, so unlike the
     * fixed/sticky-hiding pass above this can't blank a page by being too aggressive.
     */
    private static final String UNCLIP_SCROLL_CONTAINERS_SCRIPT = """
            () => {
              const unclip = (el) => {
                el.style.setProperty('overflow', 'visible', 'important');
                el.style.setProperty('height', 'auto', 'important');
                el.style.setProperty('max-height', 'none', 'important');
              };
              unclip(document.documentElement);
              unclip(document.body);
              document.querySelectorAll('body *').forEach((el) => {
                const overflowY = window.getComputedStyle(el).overflowY;
                if ((overflowY === 'auto' || overflowY === 'scroll' || overflowY === 'hidden')
                    && el.scrollHeight > el.clientHeight + 20) {
                  unclip(el);
                }
              });
            }
            """;

    /**
     * Phrases seen on pages that blocked or gated the real content instead of rendering it -
     * bot-check interstitials, consent walls, etc. Matched against the rendered page's visible
     * text (lowercased) as a heuristic for flagging a capture as likely faulty. Not exhaustive by
     * design: a false negative just means no warning shown, which is the safe direction to be
     * wrong in - the alternative (false positives on normal short postings) would be more annoying.
     */
    private static final List<String> BLOCKED_PAGE_PHRASES = List.of(
            "i'm not a robot", "i am not a robot", "verify you are human", "verify that you are human",
            "checking your browser", "just a moment", "please enable javascript and cookies",
            "quick security check", "quick check needed", "are you a robot", "access denied",
            "attention required", "captcha", "unusual traffic"
    );

    /** Below this many characters of visible text, treat a capture as suspiciously thin. */
    private static final int MIN_LIKELY_COMPLETE_LENGTH = 400;

    private final Path snapshotDir;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "playwright-snapshot");
        t.setDaemon(true);
        return t;
    });

    private Playwright playwright;
    private Browser browser;

    public PageSnapshotService(@Value("${app.snapshot-dir:data/snapshots}") String snapshotDir) {
        this.snapshotDir = Path.of(snapshotDir);
    }

    public record SnapshotResult(String filePath, Instant capturedAt, boolean likelyFaulty) {}

    public CompletableFuture<Optional<SnapshotResult>> captureAsync(UUID applicationId, String url) {
        return CompletableFuture.supplyAsync(() -> captureBlocking(applicationId, url), executor);
    }

    public Path resolveSnapshotPath(String storedFilePath) {
        return Path.of(storedFilePath);
    }

    public SnapshotResult saveManualUpload(UUID applicationId, byte[] pdfBytes) throws IOException {
        Files.createDirectories(snapshotDir);
        Path file = snapshotDir.resolve(applicationId + ".pdf");
        Files.write(file, pdfBytes);
        return new SnapshotResult(file.toString(), Instant.now(), false);
    }

    private Optional<SnapshotResult> captureBlocking(UUID applicationId, String url) {
        try {
            Files.createDirectories(snapshotDir);

            try (BrowserContext context = browser().newContext(new Browser.NewContextOptions()
                    .setViewportSize(1280, 1600)
                    .setUserAgent(DESKTOP_USER_AGENT))) {
                context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined });");

                Page page = context.newPage();
                page.navigate(url, new Page.NavigateOptions()
                        .setTimeout(NAV_TIMEOUT_MS)
                        .setWaitUntil(WaitUntilState.LOAD));
                page.waitForTimeout(RENDER_SETTLE_MS);

                // Give a JS-based bot-check ("checking your browser...") a chance to clear on its
                // own before we capture - harmless no-op on pages that don't have one.
                try {
                    page.waitForLoadState(LoadState.NETWORKIDLE,
                            new Page.WaitForLoadStateOptions().setTimeout(BOT_CHECK_TIMEOUT_MS));
                } catch (Exception ignored) {
                    // Some pages never go idle (persistent polling/websockets) - fine, we already
                    // waited RENDER_SETTLE_MS above regardless.
                }

                // Real wheel events trigger scroll-linked libraries that ignore programmatic scrollBy.
                for (int i = 0; i < 15; i++) {
                    page.mouse().wheel(0, 600);
                    page.waitForTimeout(150);
                }
                page.evaluate(AUTO_SCROLL_SCRIPT);
                page.evaluate(HIDE_FIXED_ELEMENTS_SCRIPT);
                page.evaluate(UNCLIP_SCROLL_CONTAINERS_SCRIPT);
                page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.SCREEN));

                byte[] pdf = page.pdf(new Page.PdfOptions()
                        .setFormat("A4")
                        .setPrintBackground(true));

                // Check the actual generated PDF's text, not a DOM snapshot taken moments earlier:
                // some sites swap in the real content asynchronously right around when this runs, so
                // a separately-timed DOM read can catch a stale placeholder that the PDF itself
                // doesn't have - checking the artifact itself is the only way to avoid that race.
                boolean likelyFaulty = looksFaulty(extractPdfText(pdf));

                Path file = snapshotDir.resolve(applicationId + ".pdf");
                Files.write(file, pdf);

                log.info("Captured page snapshot for application {} ({} bytes, likelyFaulty={})",
                        applicationId, pdf.length, likelyFaulty);
                return Optional.of(new SnapshotResult(file.toString(), Instant.now(), likelyFaulty));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            log.warn("Could not capture page snapshot for application {} from {}: {}", applicationId, url, e.toString());
            return Optional.empty();
        }
    }

    /** Package-private so it can be unit tested directly without driving a real browser. */
    static boolean looksFaulty(String visibleText) {
        String text = visibleText == null ? "" : visibleText.trim();
        if (text.length() < MIN_LIKELY_COMPLETE_LENGTH) {
            return true;
        }
        String lower = text.toLowerCase();
        return BLOCKED_PAGE_PHRASES.stream().anyMatch(lower::contains);
    }

    private static String extractPdfText(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        } catch (Exception e) {
            log.warn("Could not extract text from generated PDF for quality check: {}", e.toString());
            return "";
        }
    }

    private synchronized Browser browser() {
        if (browser == null) {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of("--disable-blink-features=AutomationControlled")));
        }
        return browser;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
