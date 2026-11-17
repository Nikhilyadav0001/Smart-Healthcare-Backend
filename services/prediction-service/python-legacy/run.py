import uvicorn
from src.config import PORT, HOST

if __name__ == "__main__":
    uvicorn.run("src.main:app", host=HOST, port=PORT, reload=False)
