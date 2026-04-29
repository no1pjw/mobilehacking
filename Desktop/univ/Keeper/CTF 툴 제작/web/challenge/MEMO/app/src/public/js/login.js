(function () {
  const username = document.querySelector("input[name='username']");
  const password = document.querySelector("input[name='password']");
  const button = document.querySelector("button[type='submit']");

  button.addEventListener("click", async () => {
    const { result } = await window.appCommon.getJson("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: username.value, password: password.value }),
    });

    window.appCommon.notifyByStatus(result, "Login successful.", "Login failed.");
    if (result && result.status === 200) {
      location.href = "/";
    }
  });
})();
