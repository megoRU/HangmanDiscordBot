import time

import paramiko
import sys


def main():
    print(sys.argv[1])
    print(sys.argv[2])
    print(sys.argv[3])

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(hostname=sys.argv[1], username=sys.argv[2], password=sys.argv[3], port=22)
    stdin, stdout, stderr = client.exec_command("docker-compose pull")
    time.sleep(20.0)
    stdin, stdout, stderr = client.exec_command("docker-compose up -d")
    data = stdout.read() + stderr.read()
    client.close()


if __name__ == "__main__":
    main()
