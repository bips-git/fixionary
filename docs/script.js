/* ==========================================================================
   Fixionary site — vanilla JS, no dependencies
   ========================================================================== */

/* ---------- CONFIG: edit this when new download sources go live ---------- */
const CONFIG = {
    repo: "bips-git/fixionary",
    // Paste a URL here as soon as a store lists the app. The card switches on by itself.
    links: {
        fdroid: "",      // e.g. "https://f-droid.org/packages/io.github.bips_git.fixionary"
        accrescent: "",  // e.g. "https://accrescent.app/app/io.github.bips_git.fixionary"
    },
};

const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

/* ---------- Screenshots: show a labelled placeholder if the image is missing ---------- */
document.querySelectorAll("[data-screen]").forEach((box) => {
    const img = box.querySelector("img");
    if (!img) return;
    const markMissing = () => box.classList.add("is-missing");
    img.addEventListener("error", markMissing);
    if (img.complete && img.naturalWidth === 0) markMissing();
});

/* ---------- Hero tagline: blur-morph through the key promises ---------- */
(function tagline() {
    const el = document.getElementById("morph");
    if (!el) return;
    const words = ["Offline.", "Ad-free.", "No permissions.", "Open source."];
    if (reducedMotion) {
        el.textContent = "Offline · Ad-free · No permissions · Open source";
        return;
    }
    let i = 0;
    setInterval(() => {
        if (document.hidden) return;
        el.classList.add("is-out");
        setTimeout(() => {
            i = (i + 1) % words.length;
            el.textContent = words[i];
            el.classList.remove("is-out");
        }, 420);
    }, 2400);
})();

/* ---------- Scroll reveal with a small stagger inside grids ---------- */
(function reveal() {
    const items = document.querySelectorAll(".reveal");
    if (!("IntersectionObserver" in window) || reducedMotion) {
        items.forEach((el) => el.classList.add("is-in"));
        return;
    }
    document.querySelectorAll(".grid").forEach((grid) => {
        grid.querySelectorAll(".reveal").forEach((el, n) => el.style.setProperty("--d", `${(n % 3) * 0.08}s`));
    });
    const io = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                entry.target.classList.add("is-in");
                io.unobserve(entry.target);
            }
        });
    }, { threshold: 0.12, rootMargin: "0px 0px -6% 0px" });
    items.forEach((el) => io.observe(el));
})();

/* ---------- Floating nav: hidden over the hero, scroll-spy afterwards ---------- */
(function nav() {
    const siteNav = document.querySelector(".site-nav");
    const hero = document.getElementById("home");
    if (!siteNav || !hero || !("IntersectionObserver" in window)) return;

    new IntersectionObserver(([entry]) => {
        siteNav.classList.toggle("is-visible", !entry.isIntersecting);
    }).observe(hero);

    const links = new Map(
        [...siteNav.querySelectorAll('a[href^="#"]')].map((a) => [a.getAttribute("href").slice(1), a])
    );
    const spy = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            links.forEach((a, id) => {
                const on = id === entry.target.id;
                a.classList.toggle("is-active", on);
                if (on) a.setAttribute("aria-current", "true"); else a.removeAttribute("aria-current");
            });
        });
    }, { rootMargin: "-35% 0px -55% 0px" });
    links.forEach((_, id) => {
        const section = document.getElementById(id);
        if (section) spy.observe(section);
    });
})();

/* ---------- Copy-to-clipboard buttons ---------- */
document.querySelectorAll("[data-copy]").forEach((btn) => {
    const label = btn.querySelector("span");
    const original = label ? label.textContent : "";
    btn.addEventListener("click", async () => {
        try {
            await navigator.clipboard.writeText(btn.dataset.copy);
            btn.classList.add("is-copied");
            if (label) label.textContent = "Copied!";
        } catch (err) {
            window.prompt("Copy this URL:", btn.dataset.copy);
        }
        setTimeout(() => {
            btn.classList.remove("is-copied");
            if (label) label.textContent = original;
        }, 1800);
    });
});

/* ---------- Download cards ---------- */
const LABELS = {
    fdroid: "Get it on F-Droid",
    obtainium: "Add to Obtainium",
    accrescent: "Get it on Accrescent",
    github: "View releases",
    direct: "Download APK",
};

function activate(source, href, labelText) {
    const card = document.querySelector(`.dl[data-source="${source}"]`);
    if (!card || !href) return;
    const link = card.querySelector("[data-link]");
    const pill = card.querySelector("[data-pill]");
    card.classList.remove("is-soon");
    card.classList.add("is-live");
    if (pill) {
        pill.textContent = "Available";
        pill.classList.remove("pill--soon");
        pill.classList.add("pill--live");
    }
    link.href = href;
    link.removeAttribute("aria-disabled");
    link.removeAttribute("tabindex");
    if (!href.startsWith("obtainium://")) {
        link.target = "_blank";
        link.rel = "noopener";
    }
    const labelEl = link.querySelector("[data-label]");
    if (labelEl) labelEl.textContent = labelText || LABELS[source];
    else link.textContent = labelText || LABELS[source];
}

function setMeta(name, value) {
    const el = document.querySelector(`[data-meta="${name}"]`);
    if (el && value) el.textContent = value;
}

// Store links you fill in by hand (CONFIG.links) always win.
Object.entries(CONFIG.links).forEach(([source, url]) => { if (url) activate(source, url); });

async function loadRelease() {
    const api = `https://api.github.com/repos/${CONFIG.repo}/releases/latest`;
    const cacheKey = "fixionary-latest-release";
    let data = null;
    try {
        const cached = JSON.parse(sessionStorage.getItem(cacheKey) || "null");
        if (cached && Date.now() - cached.t < 10 * 60 * 1000) data = cached.d;
    } catch (e) { /* storage unavailable */ }

    if (!data) {
        try {
            const res = await fetch(api, { headers: { Accept: "application/vnd.github+json" } });
            if (!res.ok) return;           // no release yet, or rate-limited: leave placeholders
            data = await res.json();
            try { sessionStorage.setItem(cacheKey, JSON.stringify({ t: Date.now(), d: data })); } catch (e) { /* ignore */ }
        } catch (err) {
            return;                         // offline: leave placeholders
        }
    }

    const repoUrl = `https://github.com/${CONFIG.repo}`;
    activate("github", data.html_url || `${repoUrl}/releases`);
    activate("obtainium", `obtainium://add/${repoUrl}`);

    const apks = (data.assets || []).filter((a) => /\.apk$/i.test(a.name));
    const apk = apks.find((a) => /universal|release/i.test(a.name)) || apks[0];
    if (apk) {
        const mb = (apk.size / (1024 * 1024)).toFixed(1);
        activate("direct", apk.browser_download_url, `Download APK (${mb} MB)`);
        setMeta("size", `${mb} MB`);
        if (apk.digest && apk.digest.startsWith("sha256:")) setMeta("sha", apk.digest.slice(7));
    }
    setMeta("version", data.tag_name);

    const line = document.getElementById("release-line");
    if (line && data.tag_name) {
        const when = data.published_at ? new Date(data.published_at).toLocaleDateString("en-GB", { day: "numeric", month: "long", year: "numeric" }) : "";
        line.innerHTML = "";
        const strong = document.createElement("strong");
        strong.textContent = data.tag_name;
        line.append("Latest version ", strong, when ? ` · released ${when}` : "");
        line.hidden = false;
    }
}
loadRelease();
