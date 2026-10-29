"""Serve the self-contained preview on loopback, without exposing the source tree."""
from functools import partial
from http.server import SimpleHTTPRequestHandler,ThreadingHTTPServer
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'build/preview';OUT.mkdir(parents=True,exist_ok=True)
fragment=(ROOT/'preview/star-wallpaper.html').read_text(encoding='utf-8')
(OUT/'index.html').write_text('<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>starwallpaper Preview</title><body style="margin:0;padding:16px;max-width:736px;margin-inline:auto;background:#090d14">'+fragment+'</body></html>',encoding='utf-8')
server=ThreadingHTTPServer(('127.0.0.1',0),partial(SimpleHTTPRequestHandler,directory=str(OUT)))
print(f'Preview: http://127.0.0.1:{server.server_port}/  (Ctrl+C to stop)',flush=True)
try:server.serve_forever()
except KeyboardInterrupt:pass
finally:server.server_close()
