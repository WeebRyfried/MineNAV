# Regenerates the mascot vector drawables (in-app mascot, launcher foreground and monochrome layers)
# from the logo. Usage: python branding/svg2vd.py branding/minenav.svg OsmAnd/res/drawable
import re, sys, math
src, outdir = sys.argv[1], sys.argv[2]
svg = open(src, encoding='utf-8').read()
paths = re.findall(r'<path ([^>]*)/>', svg)
items = []
for p in paths:
    d = re.search(r' ?d="([^"]*)"', ' ' + p).group(1)
    fill = re.search(r'fill="([^"]*)"', p).group(1)
    op = re.search(r'fill-opacity="([^"]*)"', p)
    items.append((d, fill, op.group(1) if op else None))
nums = [float(n) for d, _, _ in items for n in re.findall(r'-?\d+\.?\d*', d)]
xs, ys = nums[0::2], nums[1::2]  # only M/L/C pairs; H/V are rare and inside the bounds
x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
print('bbox', x0, y0, x1, y1)
NL = '\n'
def path_xml(d, fill, op, indent, color=None):
    a = f'{indent}<path{NL}{indent}\tandroid:fillColor="{color or fill}"'
    if op and not color:
        a += f'{NL}{indent}\tandroid:fillAlpha="{op}"'
    return a + f'{NL}{indent}\tandroid:pathData="{d}" />'
head = '<?xml version="1.0" encoding="utf-8"?>' + NL + '<!-- Generated from branding/minenav.svg -->' + NL
def vector(name, w, vp, body):
    open(f'{outdir}/{name}.xml', 'w', encoding='utf-8', newline='\n').write(
        head + f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{NL}\tandroid:width="{w}dp"{NL}\tandroid:height="{w}dp"{NL}'
        f'\tandroid:viewportWidth="{vp}"{NL}\tandroid:viewportHeight="{vp}">{NL}{NL}' + body + f'{NL}{NL}</vector>{NL}')
# 1. full mascot
vector('ic_minenav_mascot', 48, 2135, (NL + NL).join(path_xml(d, f, o, '\t') for d, f, o in items))
# 2. launcher foreground (108dp canvas, mascot fit inside the visible circle)
cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
s = 74 / math.hypot(x1 - x0, y1 - y0)
tx, ty = 54 - s * cx, 54 - s * cy
def group(body):
    return (f'\t<group{NL}\t\tandroid:scaleX="{s:.5f}"{NL}\t\tandroid:scaleY="{s:.5f}"{NL}'
            f'\t\tandroid:translateX="{tx:.3f}"{NL}\t\tandroid:translateY="{ty:.3f}">{NL}{NL}' + body + f'{NL}{NL}\t</group>')
vector('ic_minenav_launcher_foreground', 108, 108, group((NL + NL).join(path_xml(d, f, o, '\t\t') for d, f, o in items)))
# 3. monochrome: silhouette only (first path is the outline that covers the whole mascot)
vector('ic_minenav_launcher_monochrome', 108, 108, group(path_xml(items[0][0], None, None, '\t\t', '#FF000000')))
print('scale', s, 'translate', tx, ty)
