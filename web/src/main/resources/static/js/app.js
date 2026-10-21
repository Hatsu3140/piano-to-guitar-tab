// 「変換する」ボタンが押されたら、選んだファイルをサーバー（/api/convert）に送り、
// 返ってきた .gp5 ファイルをダウンロードする
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

    // ファイルと設定（難易度・チューニング・カポ・移調）をまとめて送る
    const formData = new FormData(form);

    submitButton.disabled = true;
    showStatus('変換中…', false);
    try {
        const response = await fetch('api/convert', { method: 'POST', body: formData });
        if (!response.ok) {
            const result = await response.json();
            showStatus(result.message, true);
            return;
        }
        const gp5Name = file.name.replace(/\.(musicxml|xml|mxl)$/i, '') + '.gp5';
        downloadFile(await response.blob(), gp5Name);
        showStatus(`${gp5Name} をダウンロードしました。TuxGuitar などで開いてください。`, false);
    } catch (error) {
        showStatus('サーバーに接続できませんでした。', true);
    } finally {
        submitButton.disabled = false;
    }
});

// 受け取ったデータを、リンクをクリックしたことにしてダウンロードさせる
function downloadFile(blob, fileName) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(url);
}

function showStatus(message, isError) {
    statusText.textContent = message;
    statusText.classList.toggle('error', isError);
}
