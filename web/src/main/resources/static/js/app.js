// 「変換する」ボタンが押されたら、選んだファイルをサーバー（/api/convert）に送る
const form = document.getElementById('upload-form');
const fileInput = document.getElementById('file-input');
const submitButton = document.getElementById('submit-button');
const statusText = document.getElementById('status');

form.addEventListener('submit', async (event) => {
    // ページが再読み込みされないようにする
    event.preventDefault();

    const file = fileInput.files[0];
    if (!file) {
        showStatus('ファイルを選んでください。', true);
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    submitButton.disabled = true;
    showStatus('変換中…', false);
    try {
        const response = await fetch('api/convert', { method: 'POST', body: formData });
        const result = await response.json();
        showStatus(`${result.message}（${result.fileName}）`, !response.ok);
    } catch (error) {
        showStatus('サーバーに接続できませんでした。', true);
    } finally {
        submitButton.disabled = false;
    }
});

function showStatus(message, isError) {
    statusText.textContent = message;
    statusText.classList.toggle('error', isError);
}
