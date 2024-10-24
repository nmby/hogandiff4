# 方眼Diff

行や列のギャップも自動で検出できるExcelファイル比較ツールです。

## 方眼Diffの特徴

特長：

- 誰でも簡単かつ直感的に利用できます。
- 行や列の挿入や削除によるギャップを自動で検出できます。
- 通常のデスクトップアプリケーションとしての利用に加えて、TortoiseGitなどのバージョン管理ツールと組み合わせて利用することもできます。
- Windows(x64)とmacOS(x64)に対応しています。

出来ること：

- Excelシート同士を比較し、内容の異なるセルを検出できます。
- 特定のシート同士、ブック同士、フォルダ同士を比較することができます。
- 行や列の挿入や削除によるギャップを自動で検出できます。
- セルの内容が数式の場合、数式文字列／計算結果の値のどちらで比較を行うかを指定できます。
- .xls/.xlsx/.xlsm形式のExcelファイルのワークシートに対応しています。異なる形式のExcelファイル同士を比較することもできます。
- 日本語・英語・簡体中国語を選択できます。

出来ないこと：

- 比較できるのはセルとセルコメントの中に記載された文字列のみです。それ以外の一切、例えばフォントの種類、セル背景色、オブジェクトなどを比較することはできません。
- .xlsb形式のExcelファイルには対応していません。また、ワークシート以外のシート（グラフシートやダイアログシートなど）には対応していません。
- 方眼Diffはシンプルで使いやすいことを最重視して設計されています。そのため、比較条件やレポート形式を細かく指定することはできません。
- Excelは高機能なアプリケーションであり、世界中で様々な用途や方法で利用されています。方眼Diffは可能な限り正しく機能することを目指していますが、対応できない場合があります。

詳細は[Webサイト](https://hogandiff.hotchpotch.xyz/)をご覧ください。

## 動作環境

次の環境で動作確認済みです。

- Windows 11 (x64)
- macOS Sonoma (x64)

方眼Diffは比較結果をテキストファイル（\*.txt）とExcelブック（\*.xls / \*.xlsx / \*.xlsm）として出力します。  
これらのファイルを表示するためのアプリケーションがお使いのPCに必要です。

## インストール方法

### Windowsの場合

ダウンロードしたzipファイルを解凍してお好きな場所に配置するだけで利用できます。

1. 「hogandiff-{version}-win-x64.zip」ファイルをダウンロードしてお好きな場所に保存します。
2. ダウンロードしたzipファイルを解凍ツールを利用して解凍します。下記のようなフォルダ階層に展開されます。
3. 解凍された「hogandiff-{version}-win-x64」フォルダを丸ごとお好きな場所に配置してお使いください。

```
hogandiff-{version}-win-x64\
  ├─ jre-min\
  │   └─ (多数のファイル群)
  ├─ lib\
  │   └─ (多数のファイル群)
  ├─ hogandiff.exe
  ├─ hogandiff.exe.vmoptions
  ├─ LICENSE.txt
  ├─ NOTICE.txt
  └─ README.md
```

「hogandiff.exe」が方眼Diffの本体です。これをダブルクリックして利用します。「hogandiff.exe」のショートカットをデスクトップなどに作成しておくと便利でしょう。

なお、「hogandiff-{version}-win-x64」フォルダの内容はそのままでご利用ください。「hogandiff.exe」だけを別の場所に取り出すと正常に動作しませんのでご注意ください。

### macOSの場合

1. 「hogandiff-{version}-mac-x64.dmg」ファイルをダウンロードしてお好きな場所に保存します。
2. ダウンロードしたdmgファイルをダブルクリックします。これによりデスクトップに「hogandiff-{version}-mac-x64」フォルダが現れます。
3. デスクトップに現れた「hogandiff-{version}-mac-x64」をダブルクリックして開きます。
4. 「方眼Diff」のアイコンを「Applications」アイコンの上にドラッグ＆ドロップします。

以上で「方眼Diff」が /Applications ディレクトリにインストールされます。
通常のアプリケーションと同様の方法で起動してお使いください。

【注意】  
方眼Diffの現在のバージョンでは、macOSが備える強固なセキュリティ保護機能に対応していません※。
そのため、方眼Diffの初回起動時にエラーメッセージが表示されます。  
手動で設定を行うことにより、セキュリティ保護機能を解除して方眼Diffを利用することができます。
Web上の各種情報を参考に、ご自身の責任でご実施ください。  
※方眼Diffの将来のバージョンで対応予定です。

## アンインストール方法

### Windowsの場合

「hogandiff-{version}-win-x64」フォルダを丸ごと削除してください。

### macOSの場合

「/Applications」ディレクトリにある「方眼Diff」を削除してください。

## 使い方

### 通常のデスクトップアプリケーションとして利用する場合

「hogandiff.exe」をダブルクリックしてアプリケーションを起動します。後はアプリケーションの画面項目に従って操作してください。比較対象のExcelファイルを2つ指定して「実行」ボタンを押下すると、比較が行われます。

### TortoiseGitなどのバージョン管理ツールと組み合わせて利用する場合

「hogandiff.exe」は比較対象のExcelファイルのパス2つをパラメータに与えることにより、コマンドラインから起動することができます。各種オプションを指定することもできます。  

```
> hogandiff.exe Excelファイルパス1 Excelファイルパス2 <オプション>
```

詳細は[Webサイト](https://hogandiff.hotchpotch.xyz/)をご覧ください。

## ライセンス

「方眼Diff」は nmby の著作物であり、MITライセンスのもと提供しています。  
詳細はこのファイル（README.md）と同じフォルダに格納されている LICENSE.txt ファイルをご参照ください。

「方眼Diff」には以下の第三者著作物が含まれており、それらのライセンス許諾に従って nmby が再頒布しています。

- OpenJDK（Eclipse Temurin Java SE）バイナリ
    - GPL v2 with Classpath Exception ライセンスにより提供されています。
    - 詳細は LICENSE.txt, NOTICE.txt 両ファイルおよび[Eclipse FoundationのWebサイト](https://projects.eclipse.org/projects/adoptium)をご参照ください。
- OpenJFX
    - GPL v2 with Classpath Exception ライセンスにより提供されています。
    - 詳細は LICENSE.txt, NOTICE.txt 両ファイルおよび[OpenJFXのWebサイト](https://openjfx.io/)をご参照ください。
- Apache POI
    - Apache License, Version 2.0 ライセンスにより提供されています。
    - 詳細は LICENSE.txt, NOTICE.txt 両ファイルおよび[Apache POIのWebサイト](https://poi.apache.org/)をご参照ください。
- Apache Log4j 2
    - Apache License, Version 2.0 ライセンスにより提供されています。
    - 詳細は LICENSE.txt, NOTICE.txt 両ファイルおよび[Apache Log4j 2のWebサイト](https://logging.apache.org/log4j/2.x/license.html)をご参照ください。

あなたが方眼Diffを利用する場合は以上の全てのライセンスに従う必要があります。

## 連絡先

e-mail  : hogandiff@hotchpotch.xyz  
website : https://hogandiff.hotchpotch.xyz/
