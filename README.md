[![GitHub release](https://img.shields.io/github/v/release/noasaba/ResourceGenerator?include_prereleases)](https://github.com/noasaba/ResourceGenerator/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub issues](https://img.shields.io/github/issues/noasaba/ResourceGenerator)](https://github.com/noasaba/ResourceGenerator/issues)
[![Last Commit](https://img.shields.io/github/last-commit/noasaba/ResourceGenerator)](https://github.com/noasaba/ResourceGenerator/commits)

# ResourceGenerator

## English Description

**ResourceGenerator** is a Minecraft plugin that automatically creates resource worlds with linked Nether and End dimensions using Multiverse-Core and Multiverse-NetherPortals.

### Features

- **Automatic Resource World Creation:**  
  Creates a new resource world (`re_world_X`), Nether (`re_world_nether_X`), and End (`re_world_the_end_X`) with automatic naming.

- **Multiverse Integration:**  
  Uses `Multiverse-Core` and `Multiverse-NetherPortals` to create and link worlds automatically.

- **Portal Auto-Linking:**  
  Sets up Nether and End portal links automatically, ensuring smooth world transitions.

- **End Respawn Handling:**  
  Configures the End's central portal to correctly return players to the corresponding resource world.

- **Configurable Initial Commands:**  
  Runs custom commands on world creation (e.g., setting game rules, difficulty levels, etc.).

### Installation

1. Download the `ResourceGenerator-1.2-SNAPSHOT.jar` file.
2. Place it into your server's `plugins` folder.
3. Restart your server or reload plugins.
4. Ensure `Multiverse-Core` and `Multiverse-NetherPortals` are installed.

### Usage

- Use `/resource create` to generate a new set of resource worlds.
- Use `/resource create -s <seed>` to generate them with a specific seed.
- The generated worlds will be linked automatically.
- The End's central portal will be set to return players correctly.
- `gamerule` and `difficulty` entries in `config.yml` are applied directly to each
  generated world. Other commands run from the console; use `{world}` where the
  generated world name must be inserted.

### Plugin Information

- **Plugin Name:** ResourceGenerator
- **Version:** 1.2-SNAPSHOT
- **API Version:** 26.2
- **Author:** nanosize

### License

This project is licensed under the GNU General Public License (GPL) version 3.  
See [LICENSE](LICENSE) for details.

---

## 日本語説明

**ResourceGenerator** は、Multiverse-Core と Multiverse-NetherPortals を使用して、
資源ワールド（現世・ネザー・エンド）を自動生成し、適切にリンク設定する Minecraft プラグインです。

### 特徴

- **自動資源ワールド生成:**  
  `re_world_X`（オーバーワールド）、`re_world_nether_X`（ネザー）、`re_world_the_end_X`（エンド）を自動命名・作成。

- **Multiverse 連携:**  
  `Multiverse-Core` と `Multiverse-NetherPortals` を活用し、ワールド作成とリンクを自動化。

- **ポータル自動リンク:**  
  ネザー・エンド間のポータルを自動的にリンク。

- **エンドのリスポーン処理:**  
  エンド中央のポータルが適切にオーバーワールドへ戻るよう設定。

- **カスタムコマンド対応:**  
  ワールド作成時に指定したゲームルールや難易度設定などを自動実行。

### インストール方法

1. `ResourceGenerator-1.2-SNAPSHOT.jar` をダウンロード。
2. サーバーの `plugins` フォルダに配置。
3. サーバーを再起動またはプラグインをリロード。
4. `Multiverse-Core` と `Multiverse-NetherPortals` が導入されていることを確認。

### 使用方法

- `/resource create` を実行すると、新しい資源ワールドが作成されます。
- `/resource create -s <シード値>` でシード値を指定できます。
- 作成されたワールドは自動でリンクされます。
- エンドの中央ポータルは、正しくオーバーワールドへ戻るよう設定されます。
- `config.yml` の `gamerule` と `difficulty` は各ワールドへ直接適用されます。
  その他のコマンドでは、生成したワールド名を挿入する位置に `{world}` を指定できます。

### プラグイン情報

- **プラグイン名:** ResourceGenerator
- **バージョン:** 1.2-SNAPSHOT
- **API バージョン:** 26.2
- **作者:** nanosize

### ライセンス

本プロジェクトは GNU General Public License (GPL) バージョン 3 の下でライセンスされています。  
詳細は [LICENSE](LICENSE) ファイルをご覧ください。
