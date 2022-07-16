const setMainContent = (element) => {
  const main = document.getElementById("main");
  main.innerHTML = "";
  main.appendChild(element);
};

const paragraph = (text) => {
  const p = document.createElement("p");
  p.textContent = text;
  return p;
};

const start = () => {
  setMainContent(paragraph("Start"));
};

const button = (text, onclick) => {
  const b = document.createElement("button");
  b.setAttribute("type", "button");
  b.textContent = text;
  b.onclick = onclick;
  return b;
};

const h1 = (text) => {
  const h = document.createElement("h1");
  h.textContent = text;
  return h;
};

const link = (text, dest) => {
  const a = document.createElement("a");
  a.setAttribute("href", dest);
  a.textContent = text;
  return a;
};

const navigate = (path) => {
  if (path.length === 0) {
    start();
  } else if (path[0] === "tune") {
    if (path.length === 1) {
      tuneIndex();
    } else if (path.length === 2 && path[1] === "new") {
      tune_new();
    } else if (path.length === 2) {
      const id = Number.parseInt(path[1]);
      if (Number.isInteger(id)) {
        tunePage(id);
      } else {
        notfound();
      }
    } else {
      notfound();
    }
  } else {
    notfound();
  }
};

const showControls = () => {
  const form = document.createElement("div");

  const heading = h1("Admin controls");
  form.appendChild(heading);

  const logoutButton = button("Log out", () => {
    fetch("api/logout", { method: "POST" }).then(checkAdminLogin);
  });
  form.appendChild(logoutButton);

  const resetButton = button("Reset database", () => {
    fetch("api/admin/reset-db", { method: "POST" });
  });
  form.appendChild(resetButton);

  setMainContent(form);
};

const showLogin = () => {
  const loginForm = document.createElement("div");

  const passwordInput = document.createElement("input");
  passwordInput.setAttribute("type", "password");
  passwordInput.setAttribute("placeholder", "admin password");
  passwordInput.setAttribute("name", "password");

  loginForm.appendChild(passwordInput);

  const sendLogin = () => {
    const body = new URLSearchParams();
    body.append("password", passwordInput.value);

    fetch("api/admin-login", {
      method: "POST",
      body: body,
    }).then(checkAdminLogin);
  };

  const loginButton = button("Log in", sendLogin);
  loginForm.appendChild(loginButton);

  setMainContent(loginForm);
};

const checkAdminLogin = () =>
  fetch("api/admin/status").then((r) => {
    if (r.status === 200) {
      showControls();
    } else {
      showLogin();
    }
  });

const main = () => {
  checkAdminLogin();
};

window.onload = main;
