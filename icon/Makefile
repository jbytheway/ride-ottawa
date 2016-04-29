SVG_LOGOS := $(wildcard logo*.svg)
PNG_LOGOS := $(addsuffix .png,$(basename $(SVG_LOGOS)))
SQUARE_PNG_LOGOS := $(addsuffix _square.png,$(basename $(SVG_LOGOS)))

all: $(PNG_LOGOS) $(SQUARE_PNG_LOGOS) feature-graphic.png

feature-graphic.png: %.png: %.svg Makefile
	inkscape -z -C -h 500 -f $< -e $@

$(PNG_LOGOS): %.png: %.svg Makefile
	inkscape -z -D -h 512 -f $< -e $@

$(SQUARE_PNG_LOGOS): %_square.png: %.png
	gm convert $< -background none -gravity center -extent 512x512 $@
