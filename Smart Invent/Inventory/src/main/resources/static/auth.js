const authState = {
  activeTab: "login",
  verificationEmail: "",
  resendTimer: null,
};

const THEME_KEY = "stockwise.theme";

const authEls = {
  tabs: document.querySelectorAll("[data-auth-tab]"),
  forms: {
    login: document.querySelector("#loginForm"),
    register: document.querySelector("#registerForm"),
    forgot: document.querySelector("#forgotPasswordForm"),
    verify: document.querySelector("#verifyForm"),
  },
  themeToggle: document.querySelector("#themeToggle"),
  themeToggleMobile: document.querySelector("#themeToggleMobile"),
  toast: document.querySelector("#authToast"),
  loginEmail: document.querySelector("#loginEmail"),
  loginPassword: document.querySelector("#loginPassword"),
  loginPasswordError: document.querySelector("#loginPasswordError"),
  loginMessage: document.querySelector("#loginMessage"),
  showVerifyButton: document.querySelector("#showVerifyButton"),
  forgotPasswordLink: document.querySelector("#forgotPasswordLink"),
  forgotPasswordEmail: document.querySelector("#forgotPasswordEmail"),
  forgotPasswordMessage: document.querySelector("#forgotPasswordMessage"),
  backToLoginButton: document.querySelector("#backToLoginButton"),
  registerName: document.querySelector("#registerName"),
  registerEmail: document.querySelector("#registerEmail"),
  registerPassword: document.querySelector("#registerPassword"),
  registerPasswordError: document.querySelector("#registerPasswordError"),
  registerMessage: document.querySelector("#registerMessage"),
  verifyEmail: document.querySelector("#verifyEmail"),
  verifyCode: document.querySelector("#verifyCode"),
  verifyMessage: document.querySelector("#verifyMessage"),
  resendCodeButton: document.querySelector("#resendCodeButton"),
};

if (sessionStorage.getItem("stockwise.token")) {
  window.location.replace("/");
}

function applyTheme(theme) {
  const nextTheme = theme === "dark" ? "dark" : "light";
  document.documentElement.dataset.theme = nextTheme;
  document.documentElement.classList.toggle("dark", nextTheme === "dark");
  localStorage.setItem(THEME_KEY, nextTheme);
  authEls.themeToggle?.setAttribute("aria-label", nextTheme === "dark" ? "Use light theme" : "Use dark theme");
  authEls.themeToggleMobile?.setAttribute("aria-label", nextTheme === "dark" ? "Use light theme" : "Use dark theme");
}

function showToast(message, type = "info") {
  if (!authEls.toast) return;
  authEls.toast.textContent = message;
  authEls.toast.classList.remove("hidden", "border-red-200", "border-green-200", "text-red-800", "text-green-800", "dark:text-red-200", "dark:text-green-200");
  if (type === "error") {
    authEls.toast.classList.add("border-red-200", "text-red-800", "dark:text-red-200");
  } else if (type === "success") {
    authEls.toast.classList.add("border-green-200", "text-green-800", "dark:text-green-200");
  }
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => {
    authEls.toast.classList.add("hidden");
  }, 3200);
}

function showFieldError(input, messageEl, message) {
  if (!input || !messageEl) return;
  input.classList.add("input-invalid");
  messageEl.textContent = message;
}

function clearFieldError(input, messageEl) {
  input?.classList.remove("input-invalid");
  if (messageEl) {
    messageEl.textContent = "";
  }
}

function getLoginPasswordError(password) {
  if (String(password || "").length < 8) {
    return "Password must be at least 8 characters.";
  }
  return "";
}

function getRegistrationPasswordError(password) {
  const value = String(password || "");
  if (value.length < 8) {
    return "Password must be at least 8 characters.";
  }
  if (/\s/.test(value)) {
    return "Password must not contain spaces.";
  }
  if (!/[A-Z]/.test(value)) {
    return "Password must include at least one uppercase letter.";
  }
  if (!/[a-z]/.test(value)) {
    return "Password must include at least one lowercase letter.";
  }
  if (!/[0-9]/.test(value)) {
    return "Password must include at least one number.";
  }
  return "";
}

function setTab(tab) {
  authState.activeTab = tab;
  authEls.tabs.forEach((button) => {
    button.classList.toggle("is-active", button.dataset.authTab === tab);
  });
  Object.entries(authEls.forms).forEach(([name, form]) => {
    form.classList.toggle("is-active", name === tab);
  });
  [authEls.loginMessage, authEls.registerMessage, authEls.forgotPasswordMessage, authEls.verifyMessage].forEach((message) => {
    if (!message) return;
    message.textContent = "";
    message.classList.remove("is-success");
  });
  clearFieldError(authEls.loginPassword, authEls.loginPasswordError);
  clearFieldError(authEls.registerPassword, authEls.registerPasswordError);
  authEls.showVerifyButton.hidden = true;
}

function clearResendCooldown() {
  window.clearInterval(authState.resendTimer);
  authState.resendTimer = null;
  authEls.resendCodeButton.disabled = false;
  authEls.resendCodeButton.textContent = "Resend Code";
}

function startResendCooldown(nextResendAt) {
  const targetMs = Date.parse(nextResendAt);
  if (!Number.isFinite(targetMs)) {
    clearResendCooldown();
    return;
  }

  const updateButton = () => {
    const secondsRemaining = Math.ceil((targetMs - Date.now()) / 1000);
    if (secondsRemaining <= 0) {
      clearResendCooldown();
      return;
    }
    authEls.resendCodeButton.disabled = true;
    authEls.resendCodeButton.textContent = `Resend Code (${secondsRemaining}s)`;
  };

  window.clearInterval(authState.resendTimer);
  updateButton();
  if (!authEls.resendCodeButton.disabled) {
    return;
  }
  authState.resendTimer = window.setInterval(updateButton, 1000);
}

function applyCooldownFromMessage(message) {
  const match = String(message || "").match(/wait\s+(\d+)\s+seconds/i);
  if (match) {
    startResendCooldown(new Date(Date.now() + Number(match[1]) * 1000).toISOString());
  }
}

function showVerification(email, message, nextResendAt) {
  authState.verificationEmail = email || authState.verificationEmail;
  authEls.verifyEmail.value = authState.verificationEmail;
  authEls.verifyCode.value = "";
  setTab("verify");
  authEls.verifyMessage.textContent = message || "Enter the code from your email to activate the account.";
  authEls.verifyMessage.classList.add("is-success");
  if (nextResendAt) {
    startResendCooldown(nextResendAt);
  } else {
    clearResendCooldown();
  }
  authEls.verifyCode.focus();
}

function saveSession(result) {
  sessionStorage.setItem("stockwise.token", result.token);
  sessionStorage.setItem("stockwise.user", JSON.stringify(result.user));
  window.location.replace("/");
}

async function authRequest(path, payload) {
  const response = await fetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    let message = `Request failed: ${response.status}`;
    try {
      const error = await response.json();
      message = error.message || message;
      if (error.fields) {
        const fieldMessage = Object.values(error.fields).find(Boolean);
        message = fieldMessage || message;
      }
    } catch {
      // Keep the status-based fallback.
    }
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return response.json();
}

async function login(event) {
  event.preventDefault();
  authEls.loginMessage.textContent = "";
  authEls.showVerifyButton.hidden = true;
  clearFieldError(authEls.loginPassword, authEls.loginPasswordError);
  const passwordError = getLoginPasswordError(authEls.loginPassword.value);
  if (passwordError) {
    showFieldError(authEls.loginPassword, authEls.loginPasswordError, passwordError);
    authEls.loginPassword.focus();
    return;
  }
  const result = await authRequest("/api/auth/login", {
    email: authEls.loginEmail.value.trim(),
    password: authEls.loginPassword.value,
  });
  saveSession(result);
}

async function register(event) {
  event.preventDefault();
  authEls.registerMessage.textContent = "";
  authEls.registerMessage.classList.remove("is-success");
  clearFieldError(authEls.registerPassword, authEls.registerPasswordError);
  const passwordError = getRegistrationPasswordError(authEls.registerPassword.value);
  if (passwordError) {
    showFieldError(authEls.registerPassword, authEls.registerPasswordError, passwordError);
    authEls.registerPassword.focus();
    return;
  }
  const result = await authRequest("/api/auth/register", {
    name: authEls.registerName.value.trim(),
    email: authEls.registerEmail.value.trim(),
    password: authEls.registerPassword.value,
  });
  authEls.registerPassword.value = "";
  showToast("Verification code sent. Check your email.", "success");
  showVerification(result.email, "Verification code sent. Check your email to activate the account.", result.nextResendAt);
}

async function requestPasswordHelp(event) {
  event.preventDefault();
  authEls.forgotPasswordMessage.textContent = "";
  authEls.forgotPasswordMessage.classList.remove("is-success");
  const email = authEls.forgotPasswordEmail.value.trim();
  if (!email) {
    authEls.forgotPasswordMessage.textContent = "Enter your registered email address.";
    authEls.forgotPasswordEmail.focus();
    return;
  }
  const result = await authRequest("/api/auth/forgot-password", { email });
  const message = result.message || "Your password update request has been submitted. Admin will contact you if the account is valid.";
  authEls.forgotPasswordMessage.textContent = message;
  authEls.forgotPasswordMessage.classList.add("is-success");
  showToast(message, "success");
}

async function verifyEmail(event) {
  event.preventDefault();
  authEls.verifyMessage.textContent = "";
  authEls.verifyMessage.classList.remove("is-success");
  const result = await authRequest("/api/auth/verify-email", {
    email: authEls.verifyEmail.value.trim(),
    code: authEls.verifyCode.value.trim(),
  });
  authEls.verifyMessage.textContent = "Email verified. Opening dashboard...";
  authEls.verifyMessage.classList.add("is-success");
  showToast("Email verified. Opening dashboard.", "success");
  window.setTimeout(() => saveSession(result), 300);
}

async function resendCode() {
  if (authEls.resendCodeButton.disabled) return;
  authEls.verifyMessage.textContent = "";
  authEls.verifyMessage.classList.remove("is-success");
  const email = authEls.verifyEmail.value.trim() || authState.verificationEmail || authEls.loginEmail.value.trim();
  if (!email) {
    authEls.verifyMessage.textContent = "Enter your email address first.";
    return;
  }
  const result = await authRequest("/api/auth/resend-verification", { email });
  showToast("A new verification code was sent.", "success");
  showVerification(result.email, "A new verification code was sent.", result.nextResendAt);
}

authEls.tabs.forEach((button) => {
  button.addEventListener("click", () => setTab(button.dataset.authTab));
});

document.querySelectorAll("[data-password-toggle]").forEach((button) => {
  button.addEventListener("click", () => {
    const input = document.querySelector(`#${button.dataset.passwordToggle}`);
    const isPassword = input.type === "password";
    input.type = isPassword ? "text" : "password";
    button.classList.toggle("is-visible", isPassword);
    button.setAttribute("aria-label", isPassword ? "Hide password" : "Show password");
  });
});

authEls.themeToggle?.addEventListener("click", () => {
  applyTheme(document.documentElement.dataset.theme === "dark" ? "light" : "dark");
});

authEls.themeToggleMobile?.addEventListener("click", () => {
  applyTheme(document.documentElement.dataset.theme === "dark" ? "light" : "dark");
});

authEls.forms.login.addEventListener("submit", (event) => {
  login(event).catch((error) => {
    authEls.loginMessage.textContent = error.message;
    showToast(error.message, "error");
    if (error.status === 403 || /verification/i.test(error.message)) {
      authEls.showVerifyButton.hidden = false;
    }
  });
});

authEls.forms.register.addEventListener("submit", (event) => {
  register(event).catch((error) => {
    authEls.registerMessage.textContent = error.message;
    showToast(error.message, "error");
  });
});

authEls.forms.forgot.addEventListener("submit", (event) => {
  requestPasswordHelp(event).catch((error) => {
    authEls.forgotPasswordMessage.textContent = error.message;
    showToast(error.message, "error");
    applyCooldownFromMessage(error.message);
  });
});

authEls.forms.verify.addEventListener("submit", (event) => {
  verifyEmail(event).catch((error) => {
    authEls.verifyMessage.textContent = error.message;
    showToast(error.message, "error");
  });
});

authEls.resendCodeButton.addEventListener("click", () => {
  resendCode().catch((error) => {
    authEls.verifyMessage.textContent = error.message;
    showToast(error.message, "error");
    applyCooldownFromMessage(error.message);
  });
});

authEls.showVerifyButton.addEventListener("click", () => {
  showVerification(authEls.loginEmail.value.trim(), "");
});

authEls.forgotPasswordLink.addEventListener("click", () => {
  authEls.forgotPasswordEmail.value = authEls.loginEmail.value.trim();
  setTab("forgot");
  authEls.forgotPasswordEmail.focus();
});

authEls.backToLoginButton.addEventListener("click", () => {
  setTab("login");
  authEls.loginEmail.focus();
});

authEls.loginPassword.addEventListener("input", () => {
  clearFieldError(authEls.loginPassword, authEls.loginPasswordError);
});

authEls.registerPassword.addEventListener("input", () => {
  clearFieldError(authEls.registerPassword, authEls.registerPasswordError);
});

applyTheme(localStorage.getItem(THEME_KEY) || document.documentElement.dataset.theme);
