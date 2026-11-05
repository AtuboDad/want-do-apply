"""Redis 连通性探活

主机端口从环境变量或 docs/.env 读取（见 _env.py）。
"""
import socket

import _env

host, port = _env.redis_conf()
s = socket.create_connection((host, port), timeout=5)
s.sendall(b"*1\r\n$4\r\nPING\r\n")
print("RESP:", s.recv(1024))
s.close()
