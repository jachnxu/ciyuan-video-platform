/**
 * 次元站 · 大模型入口注入脚本
 *
 * 背景：服务器上只有 Vue 的构建产物（assets/*.js），没有源码，
 * 无法直接改 Nav 组件。这里用「运行时注入」的方式把「大模型」按钮
 * 追加到顶部导航的 .nav-links 末尾（即「分区」之后），并守护它不被
 * Vue 的重新渲染移除。
 *
 * 关键点：主站样式是 Vue scoped CSS（选择器形如 .nav-link[data-v-xxxx]），
 * 注入的节点必须继承同样的 data-v-* 属性，否则拿不到原样式。
 */
(function () {
  "use strict";

  var AI_PATH = "/ai/";
  var MARK = "data-ai-entry";
  var pending = false;

  /** 从参考节点上复制 data-v-* 作用域属性，让注入节点复用主站 scoped 样式 */
  function inheritScope(el, ref) {
    if (!ref || !ref.attributes) return;
    for (var i = 0; i < ref.attributes.length; i++) {
      var a = ref.attributes[i];
      if (a.name.indexOf("data-v-") === 0) {
        el.setAttribute(a.name, a.value);
      }
    }
  }

  function build(ref) {
    var a = document.createElement("a");
    a.className = "nav-link ai-nav-entry";
    a.href = AI_PATH;
    a.title = "让大模型帮你找视频";
    a.setAttribute(MARK, "1");
    a.innerHTML =
      "大模型" +
      '<svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">' +
      '<path d="M12 2l1.9 5.6L19.5 9l-5.6 1.9L12 16.5l-1.9-5.6L4.5 9l5.6-1.4z"/>' +
      "</svg>";
    inheritScope(a, ref);
    return a;
  }

  function inject() {
    // 已经在 AI 页（独立页面，自带导航）就不必注入
    if (location.pathname.indexOf("/ai") === 0) return true;

    var links = document.querySelector(".nav-links");
    if (!links) return false;
    if (links.querySelector("[" + MARK + "]")) return true;

    links.appendChild(build(links.querySelector(".nav-link")));
    return true;
  }

  function schedule() {
    if (pending) return;
    pending = true;
    var run = function () {
      pending = false;
      inject();
    };
    if (window.requestAnimationFrame) requestAnimationFrame(run);
    else setTimeout(run, 16);
  }

  /** 兜底样式：万一 scoped 属性没继承成功，也不至于变成裸链接 */
  function injectFallbackCss() {
    if (document.getElementById("ai-entry-style")) return;
    var s = document.createElement("style");
    s.id = "ai-entry-style";
    s.textContent =
      ".ai-nav-entry{display:inline-flex;align-items:center;gap:3px;height:34px;" +
      "padding:0 12px;border-radius:8px;color:#61666d;font-size:14px;" +
      "text-decoration:none;transition:all .16s;white-space:nowrap}" +
      ".ai-nav-entry:hover{color:#fb7299;background:#fff1f5}";
    (document.head || document.documentElement).appendChild(s);
  }

  function boot() {
    injectFallbackCss();
    inject();
    if (window.MutationObserver) {
      // Vue 重渲染可能把注入节点冲掉，这里持续守护
      new MutationObserver(schedule).observe(document.body, {
        childList: true,
        subtree: true
      });
    }
    // 极端情况兜底（如整块 nav 被替换）
    setInterval(inject, 3000);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
