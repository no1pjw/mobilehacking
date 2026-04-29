(function () {
  const title = document.querySelector("input[name='title']");
  const content = document.querySelector("textarea[name='content']");
  const button = document.querySelector("button[type='submit']");

  button.addEventListener("click", async () => {
    const { result } = await window.appCommon.getJson("/api/memo", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ title: title.value, content: content.value }),
    });

    window.appCommon.notifyByStatus(result, "Memo created successfully.", "Failed to create memo.");
    if (result && result.status === 200) {
      location.href = "/";
    }
  });
})();
