# rts-proxy
RTSのサーバープロキシに必要な機能を追加するためのプラグインです。

## 追加機能

### Discord認証機能
サーバーに参加するためにDiscord認証を必須にしました。
DiscordBotとGoogleFirestoreを使用しているため環境変数に
`RTS_DISCORD_BOT_TOKEN`、`GOOGLE_APPLICATION_CREDENTIALS`を設定する必要があります。

### デフォルトサーバー機能
デフォルトのサーバー接続先をコマンドで変更できるようにしました。
