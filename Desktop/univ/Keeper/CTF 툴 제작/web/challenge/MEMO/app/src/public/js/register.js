(function () {
  const username = document.querySelector("input[name='username']");
  const password = document.querySelector("input[name='password']");
  const name = document.querySelector("input[name='name']");
  const button = document.querySelector("button[type='submit']");

  button.addEventListener("click", async () => {
    const { result } = await window.appCommon.getJson("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: username.value, password: password.value, name: name.value }),
    });

    window.appCommon.notifyByStatus(result, "Registration successful.", "Registration failed.");
    if (result && result.status === 200) {
      location.href = "/login";
    }
  });
})();
