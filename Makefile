CONVERTER := tools/zip2sqlite
SCRIPTS := $(wildcard tools/*.py $(CONVERTER))
DBS := app/src/main/assets/databases
ROOT := octranspo_data

$(DBS)/$(ROOT).sqlite.gz: data/$(ROOT).sqlite
	mkdir -p $(dir $@)
	gzip -c $< > $@

data/$(ROOT).sqlite: data/$(ROOT).zip $(SCRIPTS)
	$(CONVERTER) $< $@

data/$(ROOT).zip:
	mkdir -p $(dir $@)
	wget -O $@ http://www.octranspo1.com/files/google_transit.zip
