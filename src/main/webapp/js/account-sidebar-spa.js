(function () {
    'use strict';

    if (window.__pkCustomerSidebarSpaInit) return;
    window.__pkCustomerSidebarSpaInit = true;

    const loadedScripts = new Set();
    const loadedStyles = new Set();
    let navSeq = 0;

    function normPath(href) {
        try {
            return new URL(href, location.href).pathname.replace(/\/+$/, '');
        } catch {
            return '';
        }
    }

    document.querySelectorAll('script[src]').forEach(s => loadedScripts.add(normPath(s.src)));
    document.querySelectorAll('link[rel="stylesheet"][href]').forEach(l => loadedStyles.add(normPath(l.href)));
    document.head.querySelectorAll('style').forEach(st => st.setAttribute('data-uv2-dynamic', '1'));

    function getSidebarLinks() {
        return Array.from(document.querySelectorAll('.uv2-side > a[href]'));
    }

    function isDesignStudioPath(pathname) {
        const clean = pathname.replace(/\/+$/, '');
        return clean.endsWith('/design');
    }

    function getSpaAllowedPaths() {
        const paths = new Set();
        for (const a of getSidebarLinks()) {
            const p = normPath(a.href);
            if (p && !isDesignStudioPath(p)) paths.add(p);
        }
        return paths;
    }

    function syncSidebarActive(targetPathname, docSidebar) {
        const cleanTarget = targetPathname.replace(/\/+$/, '');
        const docLinks = docSidebar ? Array.from(docSidebar.querySelectorAll('a[href]')) : [];
        const docActivePaths = new Set(
            docLinks
                .filter(a => a.classList.contains('is-active') || a.classList.contains('is-current') || a.classList.contains('active'))
                .map(a => normPath(a.href))
        );

        for (const a of getSidebarLinks()) {
            const linkPath = normPath(a.href);
            const active = docActivePaths.size > 0
                ? docActivePaths.has(linkPath)
                : (linkPath === cleanTarget);
            a.classList.toggle('is-active', active);
            a.classList.toggle('is-current', active);
            a.classList.toggle('active', active);
            if (active) a.setAttribute('aria-current', 'page');
            else a.setAttribute('aria-current', 'false');

            if (docSidebar) {
                const matchDoc = docLinks.find(d => normPath(d.href) === linkPath);
                const existingCount = a.querySelector('.uv2-nav-count');
                const docCount = matchDoc && matchDoc.querySelector('.uv2-nav-count');
                if (docCount) {
                    if (existingCount) existingCount.textContent = docCount.textContent;
                    else a.appendChild(document.importNode(docCount, true));
                } else if (existingCount) {
                    existingCount.remove();
                }
            }
        }
    }

    function syncHeaderNav(doc) {
        const curNavLinks = document.querySelectorAll('#mainNav a[href]');
        const docNavLinks = Array.from(doc.querySelectorAll('#mainNav a[href]'));
        if (!curNavLinks.length || !docNavLinks.length) return;
        const activeHrefs = new Set(
            docNavLinks
                .filter(a => a.classList.contains('is-active') || a.classList.contains('active') || a.getAttribute('aria-current') === 'page')
                .map(a => normPath(a.href))
        );
        for (const a of curNavLinks) {
            const active = activeHrefs.has(normPath(a.href));
            a.classList.toggle('is-active', active);
            a.classList.toggle('active', active);
            if (active) a.setAttribute('aria-current', 'page');
            else a.removeAttribute('aria-current');
        }
    }

    function loadMissingStylesheets(doc) {
        const pending = [];
        for (const link of doc.head.querySelectorAll('link[rel="stylesheet"][href]')) {
            const path = normPath(link.getAttribute('href'));
            if (!path || loadedStyles.has(path)) continue;
            loadedStyles.add(path);
            const clone = document.createElement('link');
            clone.rel = 'stylesheet';
            clone.href = link.getAttribute('href');
            clone.setAttribute('data-uv2-dynamic-css', '1');
            pending.push(new Promise(resolve => {
                const timer = window.setTimeout(resolve, 1500);
                clone.addEventListener('load', () => { window.clearTimeout(timer); resolve(); }, { once: true });
                clone.addEventListener('error', () => { window.clearTimeout(timer); resolve(); }, { once: true });
            }));
            document.head.appendChild(clone);
        }
        return Promise.all(pending);
    }

    function syncInlineStyles(doc) {
        document.head.querySelectorAll('style[data-uv2-dynamic="1"]').forEach(el => el.remove());
        for (const styleEl of doc.head.querySelectorAll('style')) {
            if (styleEl.textContent && styleEl.textContent.includes('.uv2-profile-photo')) {
                // Keep profile photo helper style
            }
            const clone = document.createElement('style');
            clone.setAttribute('data-uv2-dynamic', '1');
            clone.textContent = styleEl.textContent;
            document.head.appendChild(clone);
        }
    }

    function loadMissingScripts(doc) {
        const pending = [];
        for (const scriptEl of doc.querySelectorAll('script[src]')) {
            const rawSrc = scriptEl.getAttribute('src');
            if (!rawSrc) continue;
            const path = normPath(rawSrc);
            if (!path || loadedScripts.has(path)) continue;
            if (path.endsWith('/header.js') || path.endsWith('/mini-cart.js') ||
                path.endsWith('/customer-chatbot.js') || path.endsWith('/account-sidebar-spa.js')) {
                loadedScripts.add(path);
                continue;
            }
            loadedScripts.add(path);
            const s = document.createElement('script');
            s.src = rawSrc;
            s.async = false;
            pending.push(new Promise(resolve => {
                s.addEventListener('load', resolve, { once: true });
                s.addEventListener('error', resolve, { once: true });
            }));
            document.body.appendChild(s);
        }
        return Promise.all(pending);
    }

    function syncMainAttributes(curMain, newMain) {
        for (const attr of Array.from(curMain.attributes)) {
            if (!newMain.hasAttribute(attr.name)) curMain.removeAttribute(attr.name);
        }
        for (const attr of Array.from(newMain.attributes)) {
            curMain.setAttribute(attr.name, attr.value);
        }
    }

    function swapMainContent(doc) {
        const curMain = document.querySelector('main.uv2-page');
        const curGrid = curMain && curMain.querySelector('.uv2-account-grid');
        const curContent = curGrid && curGrid.querySelector('.uv2-content');
        const newMain = doc.querySelector('main.uv2-page');
        const newGrid = newMain && newMain.querySelector('.uv2-account-grid');
        const newContent = newGrid && newGrid.querySelector('.uv2-content');
        if (!curMain || !curGrid || !curContent || !newMain || !newGrid || !newContent) return false;

        document.title = doc.title || document.title;
        document.body.className = doc.body.className;
        syncMainAttributes(curMain, newMain);

        // Remove nodes before curGrid and replace with nodes before newGrid
        while (curGrid.previousSibling) curGrid.previousSibling.remove();
        const beforeFrag = document.createDocumentFragment();
        for (let node = newMain.firstChild; node && node !== newGrid; node = node.nextSibling) {
            beforeFrag.appendChild(document.importNode(node, true));
        }
        curMain.insertBefore(beforeFrag, curGrid);

        // Replace .uv2-content while leaving .uv2-side intact
        curContent.replaceWith(document.importNode(newContent, true));

        // Remove nodes after curGrid (e.g. page-level <dialog> modals) and replace with nodes after newGrid
        while (curGrid.nextSibling) curGrid.nextSibling.remove();
        const afterFrag = document.createDocumentFragment();
        for (let node = newGrid.nextSibling; node; node = node.nextSibling) {
            if (node.nodeName === 'SCRIPT') continue;
            afterFrag.appendChild(document.importNode(node, true));
        }
        curMain.appendChild(afterFrag);

        syncSidebarActive(location.pathname, doc.querySelector('.uv2-side'));
        syncHeaderNav(doc);
        return true;
    }

    function reinitPageBehavior() {
        if (typeof window.__pkInitAccountModals === 'function') window.__pkInitAccountModals();
        if (typeof window.__pkInitAddressPickers === 'function') window.__pkInitAddressPickers();
        if (typeof window.__pkInitAccountPage === 'function') window.__pkInitAccountPage();
        if (typeof window.__pkInitCartPage === 'function') window.__pkInitCartPage();
        const chat = document.querySelector('.uv2-message-chat, .uv2-ticket-chat');
        if (chat) chat.scrollTop = chat.scrollHeight;
    }

    async function navigateTo(targetUrl, pushState) {
        const seq = ++navSeq;
        const fullTarget = targetUrl.pathname + targetUrl.search;
        const sameBasePath = targetUrl.pathname.replace(/\/+$/, '') === location.pathname.replace(/\/+$/, '');
        if (pushState) history.pushState({ uv2Url: fullTarget }, '', fullTarget);
        syncSidebarActive(targetUrl.pathname, null);

        const curContent = document.querySelector('.uv2-account-grid .uv2-content');
        if (curContent) curContent.style.opacity = '0.55';

        try {
            const response = await fetch(fullTarget, { credentials: 'same-origin', cache: 'no-store' });
            if (seq !== navSeq) return;
            const finalUrl = new URL(response.url || targetUrl.href, location.href);
            if (response.status === 401 || finalUrl.pathname.endsWith('/login')) {
                window.location.assign(finalUrl.href);
                return;
            }
            const html = await response.text();
            if (seq !== navSeq) return;

            const doc = new DOMParser().parseFromString(html, 'text/html');
            if (!doc.querySelector('main.uv2-page .uv2-account-grid .uv2-content')) {
                window.location.assign(finalUrl.href);
                return;
            }

            if (finalUrl.pathname + finalUrl.search !== location.pathname + location.search) {
                history.replaceState({ uv2Url: finalUrl.pathname + finalUrl.search }, '', finalUrl.pathname + finalUrl.search);
            }

            await loadMissingStylesheets(doc);
            if (seq !== navSeq) return;
            syncInlineStyles(doc);
            if (!swapMainContent(doc)) {
                window.location.assign(finalUrl.href);
                return;
            }
            await loadMissingScripts(doc);
            if (seq !== navSeq) return;
            reinitPageBehavior();
            if (!sameBasePath) window.scrollTo({ top: 0, behavior: 'auto' });
        } catch {
            if (seq === navSeq) window.location.assign(targetUrl.href);
        } finally {
            const nextContent = document.querySelector('.uv2-account-grid .uv2-content');
            if (nextContent) nextContent.style.opacity = '';
        }
    }

    document.addEventListener('click', event => {
        if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        const link = event.target.closest('a[href]');
        if (!link) return;
        if (link.target || link.download) return;

        const inSidebar = Boolean(link.closest('.uv2-side'));
        const inContentOrBreadcrumb = Boolean(link.closest('.uv2-page .uv2-content, .uv2-page .uv2-breadcrumb'));
        if (!inSidebar && !inContentOrBreadcrumb) return;

        let url;
        try { url = new URL(link.href, location.href); } catch { return; }
        if (url.origin !== location.origin) return;

        const cleanPath = url.pathname.replace(/\/+$/, '');
        if (isDesignStudioPath(cleanPath)) return;

        if (!inSidebar) {
            const allowed = getSpaAllowedPaths();
            if (!allowed.has(cleanPath)) return;
        }

        if (url.pathname === location.pathname && url.search === location.search) {
            event.preventDefault();
            return;
        }

        event.preventDefault();
        navigateTo(url, true);
    });

    document.addEventListener('submit', event => {
        const form = event.target.closest('.uv2-page .uv2-content form');
        if (!form) return;
        if ((form.getAttribute('method') || 'get').toLowerCase() !== 'get') return;
        const actionUrl = new URL(form.getAttribute('action') || location.href, location.href);
        if (actionUrl.origin !== location.origin) return;
        const allowed = getSpaAllowedPaths();
        if (!allowed.has(actionUrl.pathname.replace(/\/+$/, ''))) return;

        event.preventDefault();
        const params = new URLSearchParams(new FormData(form));
        actionUrl.search = params.toString();
        navigateTo(actionUrl, true);
    });

    window.addEventListener('popstate', () => {
        const target = new URL(location.href);
        const allowed = getSpaAllowedPaths();
        if (allowed.has(target.pathname.replace(/\/+$/, ''))) {
            navigateTo(target, false);
        }
    });
})();
