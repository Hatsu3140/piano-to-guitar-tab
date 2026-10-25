// 「変換する」ボタンが押されたら、選んだファイルをサーバー（/api/convert）に送り、
// 返ってきた .gp5 ファイルを TAB 譜として画面に表示する。ダウンロードはボタンで行う
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
const dropZone = document.getElementById('drop-zone');
const resultSection = document.getElementById('result');
const tabView = document.getElementById('tab-view');
const playButton = document.getElementById('play-button');
const stopButton = document.getElementById('stop-button');
const downloadButton = document.getElementById('download-button');
const playerStatus = document.getElementById('player-status');

// alphaTab（TAB 譜の表示と再生）のフォントと音源の置き場所。index.html で読み込んだものと同じ版にする
const ALPHATAB_BASE = 'https://cdn.jsdelivr.net/npm/@coderline/alphatab@1.8.4/dist/';

// 表示中の TAB 譜（ダウンロード用）と、alphaTab 本体（最初の表示のときに作る）
let converted = null;
let tabApi = null;

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
        converted = { blob: await response.blob(), name: gp5Name };
        showStatus('変換しました。下の TAB 譜を確かめて、よければダウンロードしてください。', false);
        showTab(await converted.blob.arrayBuffer());
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

// 変換した .gp5 を alphaTab で表示する。alphaTab が読み込めなかったとき（ネットにつながっていないなど）は、
// 表示はあきらめてダウンロードだけできるようにする
function showTab(gp5) {
    resultSection.hidden = false;
    if (typeof alphaTab === 'undefined') {
        playerStatus.textContent = 'TAB 譜を表示できませんでした（表示用の部品を読み込めませんでした）。ダウンロードはできます。';
        return;
    }
    if (!tabApi) {
        tabApi = createTabApi();
    }
    tabApi.stop();
    playButton.disabled = true;
    stopButton.disabled = true;
    playerStatus.textContent = '音源を読み込んでいます…';
    tabApi.load(new Uint8Array(gp5));
}

function createTabApi() {
    const api = new alphaTab.AlphaTabApi(tabView, {
        core: { fontDirectory: ALPHATAB_BASE + 'font/' },
        display: { scale: 0.9 },
        // 左端の楽器名は、狭い画面だと途中で切れて読めないので出さない
        notation: { elements: { trackNames: false } },
        player: {
            enablePlayer: true,
            soundFont: ALPHATAB_BASE + 'soundfont/sonivox.sf2',
            scrollElement: document.scrollingElement,
        },
    });
    // 音源の読み込みが終わったら再生できるようにする（1回目だけ時間がかかる）
    api.playerReady.on(() => {
        playButton.disabled = false;
        stopButton.disabled = false;
        playerStatus.textContent = '';
    });
    api.playerStateChanged.on((event) => {
        playButton.textContent = event.state === alphaTab.synth.PlayerState.Playing ? '一時停止' : '再生';
    });
    api.error.on(() => {
        playerStatus.textContent = 'TAB 譜の表示か再生でエラーが起きました。ダウンロードしたファイルは TuxGuitar などで開けます。';
    });
    return api;
}

playButton.addEventListener('click', () => tabApi.playPause());
stopButton.addEventListener('click', () => tabApi.stop());
downloadButton.addEventListener('click', () => downloadFile(converted.blob, converted.name));

// ファイルをドラッグ＆ドロップしても選べるようにする。ドロップしたファイルを入力欄に入れ、選んだときと同じ処理をする
dropZone.addEventListener('dragover', (event) => {
    event.preventDefault();
    dropZone.classList.add('dragging');
});
dropZone.addEventListener('dragleave', () => dropZone.classList.remove('dragging'));
dropZone.addEventListener('drop', (event) => {
    event.preventDefault();
    dropZone.classList.remove('dragging');
    if (event.dataTransfer.files.length === 0) {
        return;
    }
    fileInput.files = event.dataTransfer.files;
    fileInput.dispatchEvent(new Event('change'));
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
