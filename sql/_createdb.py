# -*- coding: utf-8 -*-
"""连接 MySQL 并直接创建 zbtech 库 + 执行 lb_community.sql"""
import pymysql

HOST = "10.10.9.171"
PORT = 3306
USER = "root"
PASSWORD = "ttk123456"
DB = "zbtech"
SQL_FILE = "F:/zslwork/test/community/sql/lb_community.sql"


def main():
    print("-> connecting to %s:%d as %s ..." % (HOST, PORT, USER))
    conn = pymysql.connect(host=HOST, port=PORT, user=USER, password=PASSWORD,
                           charset="utf8mb4", connect_timeout=10,
                           autocommit=False)
    print("-> connected.")

    with conn.cursor() as cur:
        cur.execute("CREATE DATABASE IF NOT EXISTS `%s` DEFAULT CHARACTER SET utf8mb4 "
                    "COLLATE utf8mb4_general_ci" % DB)
        print("-> database `%s` ensured." % DB)
        conn.select_db(DB)

        with open(SQL_FILE, "r", encoding="utf-8") as f:
            raw = f.read()

    # 按 ;\n 切分语句（生成文件每条语句均以 ; 结尾换行，JSON 内不含 ;\n）
    statements = []
    for chunk in raw.split(";\n"):
        s = chunk.strip()
        if not s:
            continue
        # 去掉纯注释块
        lines = [ln for ln in s.splitlines() if not ln.strip().startswith("--")]
        s2 = "\n".join(lines).strip()
        if not s2:
            continue
        statements.append(s2)

    ok, fail = 0, 0
    with conn.cursor() as cur:
        for stmt in statements:
            try:
                cur.execute(stmt)
                ok += 1
            except Exception as e:
                fail += 1
                print("  [FAIL] %s\n    -> %s" % (stmt[:80].replace("\n", " "), e))
    conn.commit()
    print("-> executed statements: ok=%d fail=%d" % (ok, fail))
    conn.close()
    print("DONE.")


if __name__ == "__main__":
    main()
