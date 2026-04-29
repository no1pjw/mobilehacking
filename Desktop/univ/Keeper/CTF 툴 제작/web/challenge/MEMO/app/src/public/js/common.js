(function () {
  const getJson = async (url, options = {}) => {
    try {
      const response = await fetch(url, options);
      const text = await response.text();
      let result = null;

      try {
        result = text ? JSON.parse(text) : null;
      } catch (error) {
        result = null;
      }

      return { response, result };
    } catch (error) {
      return { response: null, result: null };
    }
  };

  const notifyByStatus = (result, successFallback, failFallback) => {
    if (!result) {
      alert(failFallback);
      return;
    }

    if (result.status === 200) {
      alert(result.message || successFallback);
      return;
    }

    alert(result.message || failFallback);
  };

  window.appCommon = {
    getJson,
    notifyByStatus,
  };
})();
