(function () {
  const title = document.getElementById("title");
  const content = document.getElementById("content");
  const params = new URLSearchParams(window.location.search);
  const key = params.get("key");

  const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

  const fetchMemo = async () => {
    const { result } = await window.appCommon.getJson(`/api/memo/shared/${key}`);
    if (!result || result.status !== 200) {
      alert((result && result.message) || "Failed to load shared memo.");
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

  window.addEventListener("load", async () => {
    const data = await fetchMemo();
    if (!data) return;

    renderMemo(data);
    await sleep(500);
    await window.appCommon.getJson(`/api/memo/${data._id}/view`, { method: "POST" });
  });
})();
