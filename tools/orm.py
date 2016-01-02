from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, ForeignKey, Integer, String, Float

Base = declarative_base()

_converters = {
        Integer: int,
        String: (lambda x: x),
        Float: float
        }

class Mixin(object):
    def __init__(self, **kargs):
        for k, v in kargs.items():
            k = k.lstrip('\ufeff')
            if k in self.ignore_fields:
                continue
            columns = self.__table__.columns
            assert k in columns, "Missing column %r in table %r" % (
                    k, self.__tablename__)
            field_type = columns[k].type.__class__
            field_converter = _converters[field_type]
            setattr(self, k, field_converter(v))

class Stop(Mixin, Base):
    __tablename__ = 'stops'

    ignore_fields = ('stop_desc', 'stop_url', 'location_type', 'zone_id')

    stop_id = Column(String, primary_key=True)
    stop_code = Column(String, index=True)
    stop_name = Column(String)
    stop_lat = Column(Float)
    stop_lon = Column(Float)

class Route(Mixin, Base):
    __tablename__ = 'routes'

    ignore_fields = ('route_long_name', 'route_desc', 'route_type', 'route_url')

    route_id = Column(String, primary_key=True)
    route_short_name = Column(String, index=True)

class Trip(Mixin, Base):
    __tablename__ = 'trips'

    ignore_fields = ()

    trip_id = Column(String, primary_key=True)
    route_id = Column(String, ForeignKey('routes.route_id'), index=True)
    service_id = Column(String)
    trip_headsign = Column(String)
    direction_id = Column(Integer)
    block_id = Column(Integer)

class StopTime(Mixin, Base):
    __tablename__ = 'stop_times'

    ignore_fields = ('departure_time')

    id = Column(Integer, primary_key=True)
    trip_id = Column(String, ForeignKey('trips.trip_id'), index=True)
    arrival_time = Column(String)
    stop_id = Column(String, ForeignKey('stops.stop_id'), index=True)
    stop_sequence = Column(Integer)
    pickup_type = Column(Integer)
    drop_off_type = Column(Integer)

