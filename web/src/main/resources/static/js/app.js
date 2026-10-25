// 「変換する」ボタンが押されたら、選んだファイルをサーバー（/api/convert）に送り、
// 返ってきた .gp5 ファイルをダウンロードする
const form = document.getElementById('upload-form');
const fileInput = document.getElementById('file-input');
const submitButton = document.getElementById('submit-button');
const statusText = document.getElementById('status');
const reportList = document.getElementById('report');
const barreCheck = document.getElementById('barre');
const strokesCheck = document.getElementById('strokes');
const tuningSelect = document.getElementById('tuning');
const capoSelect = document.getElementById('capo');
const transposeInput = document.getElementById('transpose');
const suggestionsSection = document.getElementById('suggestions');
const suggestionStatus = document.getElementById('suggestion-status');
const suggestionList = document.getElementById('suggestion-list');

// 何回目に調べ始めたか。設定を続けて変えたとき、古い結果で上書きしないために使う
let suggestRequest = 0;

// ファイルや設定を変えたら、曲に合うチューニングとカポを調べ直す
fileInput.addEventListener('change', suggestSetup);
transposeInput.addEventListener('change', suggestSetup);
barreCheck.addEventListener('change', suggestSetup);
strokesCheck.addEventListener('change', suggestSetup);

form.addEventListener('submit', async (event) => {
    // ページが再読み込みされないようにする
    event.preventDefault();

    const file = fileInput.files[0];
    if (!file) {
        showStatus('ファイルを選んでください。', true);
        return;
    }

    // ファイルと設定（チューニング・カポ・移調・奏法）をまとめて送る。
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

// サーバー（/api/suggest）に、曲に合うチューニングとカポを調べてもらう。
// 一番のおすすめを設定欄に入れ、候補を一覧にして、クリックで選び直せるようにする
async function suggestSetup() {
    const file = fileInput.files[0];
    if (!file) {
        suggestionsSection.hidden = true;
        return;
    }
    const request = ++suggestRequest;
    const formData = new FormData();
    formData.set('file', file);
    formData.set('transpose', transposeInput.value);
    formData.set('barre', barreCheck.checked);
    formData.set('strokes', strokesCheck.checked);

    suggestionsSection.hidden = false;
    suggestionStatus.textContent = 'すべての組み合わせで試しています…（数秒かかります）';
    suggestionList.replaceChildren();
    submitButton.disabled = true;
    try {
        const response = await fetch('api/suggest', { method: 'POST', body: formData });
        const result = await response.json();
        if (request !== suggestRequest) {
            return; // もっと新しい調べ直しが始まっている
        }
        if (!response.ok) {
            suggestionStatus.textContent = result.message;
            return;
        }
        suggestionStatus.textContent = 'クリックすると、その設定に変えられます。';
        showSuggestions(result.suggestions);
        if (result.suggestions.length > 0) {
            applySuggestion(result.suggestions[0]);
        }
    } catch (error) {
        if (request === suggestRequest) {
            suggestionStatus.textContent = 'サーバーに接続できませんでした。';
        }
    } finally {
        if (request === suggestRequest) {
            submitButton.disabled = false;
        }
    }
}

function showSuggestions(suggestions) {
    suggestionList.replaceChildren(...suggestions.map((suggestion, index) => {
        const item = document.createElement('li');
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'suggestion';
        button.dataset.tuning = suggestion.tuning;
        button.dataset.capo = suggestion.capo;

        const title = document.createElement('span');
        title.className = 'suggestion-title';
        title.textContent = (index === 0 ? 'いちばんのおすすめ：' : '') + describeSetup(suggestion);
        const reasons = document.createElement('span');
        reasons.className = 'suggestion-reasons';
        reasons.textContent = suggestion.reasons.join('、');
        button.append(title, reasons);

        button.addEventListener('click', () => applySuggestion(suggestion));
        item.append(button);
        return item;
    }));
}

// 提案を設定欄（チューニング・カポ）に入れ、選んでいる候補に印を付ける
function applySuggestion(suggestion) {
    tuningSelect.value = suggestion.tuning;
    capoSelect.value = String(suggestion.capo);
    markChosenSuggestion();
}

// 設定欄と同じ組み合わせの候補に印を付ける（手で設定を変えたときも合わせる）
function markChosenSuggestion() {
    for (const button of suggestionList.querySelectorAll('.suggestion')) {
        const chosen = button.dataset.tuning === tuningSelect.value && button.dataset.capo === capoSelect.value;
        button.classList.toggle('chosen', chosen);
    }
}
tuningSelect.addEventListener('change', markChosenSuggestion);
capoSelect.addEventListener('change', markChosenSuggestion);

// 例: 「レギュラー（EADGBE）・カポ 3フレット」。名前は設定欄の選択肢の文字をそのまま使う
function describeSetup(suggestion) {
    const tuningOption = tuningSelect.querySelector(`option[value="${suggestion.tuning}"]`);
    const capoOption = capoSelect.querySelector(`option[value="${suggestion.capo}"]`);
    const tuningName = tuningOption ? tuningOption.textContent : suggestion.tuning;
    const capoName = suggestion.capo === 0 ? 'カポなし' : `カポ ${capoOption.textContent}`;
    return `${tuningName}・${capoName}`;
}

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
