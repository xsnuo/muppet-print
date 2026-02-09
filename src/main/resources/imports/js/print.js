var printingWidth = printingWidth || w;
var printingHeight = printingHeight || h;

// 设置条
const printSettingsDiv = document.createElement('div');
document.body.insertBefore(printSettingsDiv, document.body.firstChild);
printSettingsDiv.innerHTML = `
<div class="no-print" style="text-align: center;">
    <span style="font-size: 16px;">页面宽度(MM)</span>
    <input type="number" v-model="_pageWidth" style="width:100px;" />
    <span style="font-size: 16px;">页面高度(MM)</span>
    <input type="number" v-model="_pageHeight" style="width:100px;" />
    <button @click="_useSettings()">应用</button>
    <button @click="_printPage()">打印</button>
</div>
<div class="no-print" style="height: 5rem;"></div>
`;

// 重置页面尺寸的方法
var resetPageSize = (w, h, pw, ph) => {
    // 页面宽高
    const pages = document.querySelectorAll('.print-page');
    pages.forEach(page => {
        page.style.width = (pw - 1) + 'mm';
        if (page.classList.contains('page-over')) {
            page.style.minHeight = (ph - 1) + 'mm';
        } else {
            page.style.height = (ph - 1) + 'mm';
        }
    });
    // 灰色背景宽
    const boxes = document.querySelectorAll('.print-box');
    boxes.forEach(box => {
        box.style.minWidth = (pw - 1) + 'mm';
    });
    // rem缩放比例
    const fontSizeW = pw / w;
    const fontSizeH = ph / h;
    document.documentElement.style.fontSize = Math.min(fontSizeW, fontSizeH) + 'mm';
};

// 初始化VUE，渲染数据，绑定事件
const app = PetiteVue.createApp({
    ...d,
    ...f,
    _pageWidth: printingWidth,
    _pageHeight: printingHeight,
    _useSettings() {
        resetPageSize(w, h, this._pageWidth, this._pageHeight);
        resetQr();
        resetBr();
    },
    _printPage() {
        window.print();
    }
});

// 刷新二维码
var resetQr = () => {
    const qrDivs = document.querySelectorAll('.qr');
    qrDivs.forEach(div => {
        const qrText = div.getAttribute('data-value');
        div.innerHTML = '';
        new QRCode(div, {
            text: qrText,
            width: div.clientWidth,
            height: div.clientHeight,
            correctLevel: QRCode.CorrectLevel.H
        });
    });
};

// 刷新条形码
var resetBr = () => {
    JsBarcode(".barcode").init();// 方式1：svg
    const barcodeDivs = document.querySelectorAll('.br');// 方式2：div
    barcodeDivs.forEach(div => {
        const brText = div.getAttribute('data-value');
        div.innerHTML = '<svg></svg>';
        const svg = div.querySelector('svg');
        var barWidth = div.clientWidth / brText.length / 11 - 0.1;// 估算二维码单列宽
        if (barWidth > 2) {
            barWidth = 2;// 最大是2，防止过宽
        }
        do {
            JsBarcode(svg, brText, {
                width: barWidth,
                height: div.clientHeight,
                displayValue: false,
                margin: 0
            });
            barWidth = barWidth - 0.2;
        } while (svg.getBBox().width > div.clientWidth && barWidth > 0);
    });
};

if (f.init) {
    f.init();
}
resetPageSize(w, h, printingWidth, printingHeight);
app.mount();
resetQr();
resetBr();
window.printReady = true;