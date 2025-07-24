let container = document.getElementById('container')
let signUpText = document.querySelector('.text.sign-up');

toggle = () => {
  container.classList.toggle('sign-in');
  container.classList.toggle('sign-up');
  // sign-up 상태면 보이게, 아니면 숨김
  if (container.classList.contains('sign-up')) {
    if (signUpText) signUpText.style.display = 'block';
  } else {
    if (signUpText) signUpText.style.display = 'none';
  }
}

setTimeout(() => {
  container.classList.remove('sign-up');
  container.classList.add('sign-in');
  if (signUpText) signUpText.style.display = 'none';
}, 200);