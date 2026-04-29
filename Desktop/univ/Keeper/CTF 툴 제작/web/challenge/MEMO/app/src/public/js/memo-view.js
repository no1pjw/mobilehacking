(function () {
  const title = document.getElementById("title");
  const content = document.getElementById("content");
  const shareButton = document.getElementById("share");
  const params = new URLSearchParams(window.location.search);
  const id = params.get("id");

  const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

  const fetchMemo = async () => {
    const { result } = await window.appCommon.getJson(`/api/memo/${id}`);
    if (!result || result.status !== 200) {
      alert((result && result.message) || "Failed to load memo.");
      location.href = "/";
      return null;
    }

    return result.data;
  };

  const renderMemo = (data) => {
    const option = {
      ALLOWED_TAGS: [
        'b', 'strong', 'i', 'em', 'u', 's', 'del',
        'p', 'br', 'ul', 'ol', 'li', 'span', 'img'
      ],
    };
    const sanitizedTitle = DOMPurify.sanitize(data.title, option);
    const sanitizedContent = DOMPurify.sanitize(data.content, option);

    title.innerHTML = sanitizedTitle;
    content.innerHTML = sanitizedContent;
  };

  shareButton.addEventListener("click", async () => {
    const { result } = await window.appCommon.getJson(`/api/memo/${id}/share`, {
      method: "POST",
    });

    window.appCommon.notifyByStatus(result, "Memo shared successfully.", "Failed to share memo.");
    if (result && result.status === 200 && result.data && result.data.sharedKey) {
      location.href = `/memo/shared?key=${result.data.sharedKey}`;
    }
  });

  window.addEventListener("load", async () => {
    const data = await fetchMemo();
    if (!data) return;

    renderMemo(data);
    await sleep(500);
    await window.appCommon.getJson(`/api/memo/${id}/view`, { method: "POST" });
  });
})();
