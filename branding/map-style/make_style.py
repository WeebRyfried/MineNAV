# Generates MineNAV.style.xml, the MineNAV map style, as a copy of the OsmAnd default style with new colors and fewer labels.
# Usage (from the repo root, with OsmAnd-resources cloned next to it):
#   python branding/map-style/make_style.py ../resources/rendering_styles/default.render.xml
import re
import sys

source = open(sys.argv[1], encoding='utf-8').read()
attrs = re.findall(r'<renderingAttribute name="([^"]*)"', source)
T = '#00000000'
greenery = {'woodColor', 'forestColor', 'natureReserveParkColor', 'parkColor', 'grassColor', 'meadowColor', 'greeneryColor',
	'recreationGroundColor', 'villageGreenColor', 'gardenColor', 'golfCourseColor', 'cemeteryColor', 'grasslandColor', 'heathColor'}
# every land-use / building / amenity fill except greenery and water becomes invisible
area_like = [a for a in attrs if a.endswith('Color') and re.match(r'(landuse|building|amenity|leisure|farm|flowerbed|brownfield|greenfield|lava|desert|'
	r'orchard|attraction|aerodrome|apron|aeroway|taxiway|pier|parking|bicycleParking|port|power|stadium|stands|pitch|playground|sportsCentre|'
	r'themepark|golf|church|spoilHeap|primaryResidentialHighwayArea|serviceHighwayArea|abandonedLanduse|natureReserveStroke|parkStroke|moreDetailedScrub|'
	r'boundary|leisureResort|landuseMilitary)', a) and a not in greenery and 'Text' not in a]
day_night = [(a, T, T) for a in area_like]
day_night += [(a, '#D4EDC9', '#1C3236') for a in sorted(greenery)]
day_night.append(('waterColor', '#AAD8F6', '#15304F'))
# roads: white with grey casing up close, plain grey lines when zoomed out
roads = [('motorway', '#9C9C9C', '#A3A3A3'), ('trunk', '#A7A7A7', '#ADADAD'), ('primary', '#B4B4B4', '#BBBBBB'),
	('secondary', '#C2C2C2', '#C8C8C8'), ('tertiary', '#C9C9C9', '#D0D0D0')]
for r, casing, low in roads:
	day_night += [(f'{r}RoadColor', '#FFFFFF', '#4A6180'), (f'{r}RoadShadowColor', casing, '#2C3B4F'), (f'{r}RoadLowZoomColor', low, '#4A6180')]
day_night += [('tertiaryRoadLowZoomShadowColor', '#C9C9C9', '#2C3B4F'), ('residentialRoadColor', '#FFFFFF', '#3E536B'),
	('residentialRoadShadowColor', '#D2D2D2', '#283646'), ('residentialRoadLowZoom1ShadowColor', '#D2D2D2', '#283646'),
	('residentialRoadLowZoom2ShadowColor', '#D2D2D2', '#283646'), ('serviceRoadColor', '#FFFFFF', '#384B61'), ('serviceRoadShadowColor', '#DADADA', '#283646'),
	('pedestrianRoadColor', '#F7F7F7', '#2C3241'), ('pedestrianRoadShadowColor', '#E2E2E2', '#242935'),
	('footwayColor', '#E0E0E0', '#2E3443'), ('pathColor', '#E0E0E0', '#2E3443'), ('pathLowZoomColor', '#E6E6E6', '#2E3443'),
	('cyclewayColor', '#E0E0E0', '#2E3443'), ('trackColor', '#E0E0E0', '#2E3443'), ('bridlewayColor', '#E0E0E0', '#2E3443'),
	('stepsRampColor', '#E0E0E0', '#2E3443'), ('railwayRailColor', '#CFCFCF', '#3A4050'), ('railwayRailColor2', '#F4F4F2', '#1E2735')]
day_night += [(a, T, T) for a in attrs if a.endswith('HighwayOnewayArrowsColor')]
# labels: calm dark grey
for a in ['motorwayTextColor', 'trunkTextColor', 'primaryTextColor', 'secondaryTextColor', 'tertiaryTextColor', 'residentialTextColor',
	'pedestrianTextColor', 'placeCityVillageTextColor', 'placeSuburbHamletTextColor']:
	day_night.append((a, '#4A4F5C', '#A9BCD0'))
day_night += [('placeCityVillageTextHaloColor', '#F4F4F2', '#1E2735'), ('residentialTextHaloColor', '#FFFFFF', '#1E2735'),
	('waterTextColor', '#4F86B5', '#8FB8DA')]
missing = [a for a, _, _ in day_night if a not in attrs]
assert not missing, missing

# names: like Waze, only roads, places and water keep their labels. The text rules for everything else get a tag no map
# object has, so they never match and nothing is written.
named_tags = {'amenity', 'shop', 'office', 'tourism', 'leisure', 'historic', 'man_made', 'craft', 'healthcare', 'building',
	'landuse', 'club', 'emergency', 'military', 'sport', 'power', 'railway', 'public_transport', 'aerialway', 'barrier',
	'cemetery', 'religion', 'diplomatic', 'education', 'service', 'route', 'piste:type', 'telecom'}
text_start = source.index('\n\t<text>')
text_end = source.index('</text>', text_start)
hidden = 0


def unlabel(match):
	global hidden
	if match.group(1) not in named_tags:
		return match.group(0)
	hidden += 1
	return f'tag="minenav_unlabelled_{match.group(1)}" value="{match.group(2)}"'


text = re.sub(r'tag="([^"]*)" value="([^"]*)"', unlabel, source[text_start:text_end])
style = source[:text_start] + text + source[text_end:]

# a standalone copy of the default style: the OpenGL renderer does not let a depending style replace the default rules
style = style.replace('<renderingStyle name="default" depends="" defaultColor="#ebe7e4"',
	'<renderingStyle name="MineNAV" depends="" defaultColor="#F4F4F2"', 1)
assert 'name="MineNAV"' in style


def attribute(name, cases):
	return f'<renderingAttribute name="{name}">\n' + ''.join(f'\t\t{c}\n' for c in cases) + '\t</renderingAttribute>'


blocks = {'defaultColor': ['<case noPolygons="true" attrColorValue="#00F4F4F2"/>', '<case nightMode="true" attrColorValue="#1E2735"/>',
	'<case attrColorValue="#F4F4F2"/>'],
	# Waze-like purple route line; color_0 is the outline, color_2 the direction arrows, color_3 the turn arrows
	'route': ['<case color="#CC7B3FF2" strokeWidth="12:8" color_0="#E05B21B6" strokeWidth_0="14:8" color_2="#E9DDFF" color_3="#FFFFFF" strokeWidth_3="5:7">',
		'\t<apply_if nightMode="true" color="#CCA78BFA" color_0="#E06D28D9" color_2="#3B2A66" color_3="#FFFFFF"/>', '</case>']}
for name, day, night in day_night:
	if name not in blocks:
		blocks[name] = [f'<case attrColorValue="{day}"/>'] if day == night else \
			[f'<case nightMode="true" attrColorValue="{night}"/>', f'<case attrColorValue="{day}"/>']
for name, cases in blocks.items():
	pattern = re.compile(r'<renderingAttribute name="' + re.escape(name) + r'">.*?</renderingAttribute>', re.S)
	style, count = pattern.subn(lambda m: attribute(name, cases), style, count=1)
	assert count == 1, name

header = ('<!-- MineNAV map style: a clean, Waze-like copy of the OsmAnd default style. Only water and parks keep a color,\n'
	'     roads are white with grey outlines, labels are calm grey and only roads, places and water are labelled.\n'
	'     Copied into the app as MineNAV.render.xml by OsmAnd-java/build.gradle.\n'
	'     Generated by branding/map-style/make_style.py; do not edit by hand. -->\n')
style = style.replace('<renderingStyle name="MineNAV"', header + '<renderingStyle name="MineNAV"', 1)
open('branding/map-style/MineNAV.style.xml', 'w', encoding='utf-8', newline='\n').write(style)
print(len(blocks), 'attributes,', len(area_like), 'areas hidden,', hidden, 'label rules disabled')
