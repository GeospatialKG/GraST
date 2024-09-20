from fastapi import FastAPI, status
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse, RedirectResponse
from DBConnect.connectNeo import router as neo_router
from DBConnect.connectPG import router as pg_router
from GEOImport.geoshp import router as shp_router
from GEOImport.geotif import router as tif_router
import os

app = FastAPI()

# 设置静态文件目录
static_files_path = "Client"
app.mount("/GraST/static", StaticFiles(directory=static_files_path), name="static")

@app.get("/")
async def root():
    return RedirectResponse(url="/GraST", status_code=status.HTTP_307_TEMPORARY_REDIRECT)

@app.get("/GraST", response_class=HTMLResponse)
async def main():
    index_path = os.path.join(static_files_path, "index.html")
    with open(index_path, "r", encoding="utf-8") as f:
        html_content = f.read()
    return HTMLResponse(content=html_content)
app.include_router(neo_router)
app.include_router(pg_router)
app.include_router(shp_router)
app.include_router(tif_router)