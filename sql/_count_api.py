import re, os, glob

ROOT = "C:/Users/Administrator/Desktop/foxbook-master/foxbook-php/app"

php_files = glob.glob(ROOT + "/**/*.php", recursive=True)

classes = {}
file_main_class = {}

for f in php_files:
    try:
        txt = open(f, encoding='utf-8', errors='ignore').read()
    except Exception:
        continue
    cm = re.search(r'\bclass\s+(\w+)(?:\s+extends\s+(\w+))?', txt)
    if not cm:
        continue
    cname = cm.group(1)
    ext = cm.group(2)
    classes.setdefault(cname, {'extends': None, 'public': set()})
    classes[cname]['extends'] = ext
    file_main_class[f] = cname
    for mm in re.finditer(r'^\s*(?:final\s+|abstract\s+)?(public|protected|private)\s+function\s+(\w+)', txt, re.M):
        if mm.group(1) == 'public':
            classes[cname]['public'].add(mm.group(2))

def collect(cname, seen, out):
    if cname in seen or cname not in classes:
        return
    seen.add(cname)
    out |= classes[cname]['public']
    ext = classes[cname]['extends']
    if ext:
        collect(ext, seen, out)

EXCLUDE = {'success', 'fail', '__construct', '__destruct', '__call', '__get', '__set'}

def api_interfaces(cname):
    out = set()
    collect(cname, set(), out)
    return sorted(out - EXCLUDE)

def count_module(keyword):
    rows = []
    total = 0
    for f, cname in file_main_class.items():
        nf = f.replace('\\', '/')
        if keyword == 'api' and ('/api/controller/' not in nf or '/adminapi/' in nf):
            continue
        if keyword == 'adminapi' and '/adminapi/controller/' not in nf:
            continue
        if cname in ('BaseApi', 'BaseAdminApi'):
            continue
        ifs = api_interfaces(cname)
        if not ifs:
            continue
        rows.append((cname, ifs))
        total += len(ifs)
    return rows, total

api_rows, api_total = count_module("api")
adm_rows, adm_total = count_module("adminapi")

print("================= /api 用户端接口 =================")
for name, ifs in api_rows:
    print(f"  [{name}] ({len(ifs)}): {', '.join(ifs)}")
print(f"  --- api 小计: {api_total} 个接口，{len(api_rows)} 个控制器 ---")

print("\n================= /adminapi 管理端接口 =================")
for name, ifs in adm_rows:
    print(f"  [{name}] ({len(ifs)}): {', '.join(ifs)}")
print(f"  --- adminapi 小计: {adm_total} 个接口，{len(adm_rows)} 个控制器 ---")

print(f"\n================= 合计 =================")
print(f"  控制器总数: {len(api_rows) + len(adm_rows)}")
print(f"  接口总数:   {api_total + adm_total}")

# PC 端页面路由（route.php）作为补充参考
route_file = "C:/Users/Administrator/Desktop/foxbook-master/foxbook-php/config/route.php"
try:
    rt = open(route_file, encoding='utf-8', errors='ignore').read()
    pc_routes = re.findall(r'Route::\w+\(', rt)
    print(f"\n[补充] PC 前端页面路由 (config/route.php): {len(pc_routes)} 条（非 API）")
except Exception as e:
    print("route.php read err", e)
