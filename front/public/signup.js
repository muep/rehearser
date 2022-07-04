const messageSpan = document.getElementById("message");
const usernameInput = document.getElementById("username-input");
const submitButton = document.getElementById("submit-button");

const validateUsername = (name) => {
  if (name.length < 4) {
    return "user name must have at least four characters";
  }

  if (name.length > 30) {
    return "user name length may not exceed 30 characters";
  }

  return "";
};

const validate = () => {
  const nameProblem = validateUsername(usernameInput.value);

  if (nameProblem.length !== 0) {
    return nameProblem;
  }

  return "";
};

const validateAndReport = () => {
  const problem = validate();
  messageSpan.textContent = problem;
  submitButton.disabled = problem.length !== 0;
};

const form = document.getElementById("signup-form");
form.addEventListener("change", validateAndReport);
