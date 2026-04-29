(function () {
  const stateTitle = document.getElementById("stateTitle");
  const stateBadge = document.getElementById("stateBadge");
  const stateDescription = document.getElementById("stateDescription");
  const username = document.getElementById("username");
  const memoCount = document.getElementById("memoCount");
  const guestActions = document.getElementById("guestActions");
  const memberActions = document.getElementById("memberActions");
  const homeMemoSection = document.getElementById("homeMemoSection");
  const homeMemos = document.getElementById("homeMemos");

  const renderMemos = (memos) => {
    homeMemos.innerHTML = "";
    if (!memos || memos.length === 0) {
      const li = document.createElement("li");
      li.textContent = "No memos yet. Create your first memo now.";
      li.className = "preview";
      homeMemos.appendChild(li);
      return;
    }

    memos.forEach((memo) => {
      const li = document.createElement("li");
      const a = document.createElement("a");
      a.textContent = memo.title;
      a.href = `/memo/view?id=${memo._id}`;
      li.appendChild(a);
      homeMemos.appendChild(li);
    });
  };

  const setGuestState = () => {
    stateTitle.textContent = "Visitor Mode";
    stateBadge.textContent = "Guest";
    stateBadge.classList.remove("ok");
    stateDescription.textContent = "Sign in to manage and share your memos.";
    username.textContent = "Guest";
    memoCount.textContent = "0";
    guestActions.classList.remove("hidden");
    memberActions.classList.add("hidden");
    homeMemoSection.classList.add("hidden");
  };

  const setMemberState = (user, memos) => {
    stateTitle.textContent = "Signed In";
    stateBadge.textContent = "Logged In";
    stateBadge.classList.add("ok");
    stateDescription.textContent = "You are signed in. Use the actions below to manage your memos.";
    username.textContent = user.username;
    memoCount.textContent = String((memos || []).length);
    guestActions.classList.add("hidden");
    memberActions.classList.remove("hidden");
    homeMemoSection.classList.remove("hidden");
    renderMemos(memos || []);
  };

  const bootstrap = async () => {
    const { response, result } = await window.appCommon.getJson("/api/user");
    if (!response || response.status !== 200 || !result || result.status !== 200 || !result.data) {
      setGuestState();
      return;
    }

    setMemberState(result.data.user, result.data.memos);
  };

  bootstrap();
})();
