from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer, String, Float

Base = declarative_base()

_converters = {
        Integer: int,
        String: (lambda x: x),
        Float: float
        }

class Mixin(object):
    id = Column(Integer, primary_key=True)

    def __init__(self, **kargs):
        for k, v in kargs.items():
            if k in self.ignore_fields:
                assert v in ('', '0'), 'Ignoring %r = %r' % (k, v)
                continue
            field_type = self.__table__.columns[k].type.__class__
            field_converter = _converters[field_type]
            setattr(self, k, field_converter(v))

class Stop(Mixin, Base):
    __tablename__ = 'stops'

    ignore_fields = ('stop_desc', 'stop_url', 'location_type', 'zone_id')

    stop_id = Column(String)
    stop_code = Column(String)
    stop_name = Column(String)
    stop_lat = Column(Float)
    stop_lon = Column(Float)
    
