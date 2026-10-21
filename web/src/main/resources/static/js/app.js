// 「変換する」ボタンが押されたら、選んだファイルをサーバー（/api/convert）に送り、
// 返ってきた .gp5 ファイルをダウンロードする
const form = document.getElementById('upload-form');
const fileInput = document.getElementById('file-input');
const submitButton = document.getElementById('submit-button');
const statusText = document.getElementById('status');
const reportList = document.getElementById('report');
const barreCheck = document.getElementById('barre');
const strokesCheck = document.getElementById('strokes');

form.addEventListener('submit', async (event) => {
    // ページが再読み込みされないようにする
    event.preventDefault();

    const file = fileInput.files[0];
    if (!file) {
        showStatus('ファイルを選んでください。', true);
        return;
    }

    // ファイルと設定（難易度・チューニング・カポ・移調・奏法）をまとめて送る。
    // チェックボックスはチェックを外すと送られないので、true / false をはっきり入れる
    const formData = new FormData(form);
    formData.set('barre', barreCheck.checked);
    formData.set('strokes', strokesCheck.checked);

    submitButton.disabled = true;
    showStatus('変換中…', false);
    showReport([]);
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
        const report = response.headers.get('X-Conversion-Report');
        showReport(report ? decodeURIComponent(report).split('\n') : []);
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

// 変換レポート（何音にしたか、省いた音など）を箇条書きで表示する
function showReport(lines) {
    reportList.replaceChildren(...lines.map((line) => {
        const item = document.createElement('li');
        item.textContent = line;
        return item;
    }));
}

function showStatus(message, isError) {
    statusText.textContent = message;
    statusText.classList.toggle('error', isError);
}
