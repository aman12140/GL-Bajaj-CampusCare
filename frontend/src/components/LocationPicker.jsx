import { useEffect, useMemo, useState } from 'react';

/**
 * Structured location selection: Location type -> Building/Block -> Floor -> Room/Area.
 * Room / Area is entered manually so students can report any room, lab or area
 * even when it has not been pre-configured in the location list.
 */
export default function LocationPicker({ locations, onChange, onRoomChange, roomNumber = '', roomError, error }) {
  const [type, setType] = useState('');
  const [buildingId, setBuildingId] = useState('');
  const [floorId, setFloorId] = useState('');

  const buildings = useMemo(() => locations.filter((l) => l.level === 'BUILDING'), [locations]);
  const types = useMemo(() => [...new Set(buildings.map((b) => b.type))].sort(), [buildings]);
  const typeBuildings = buildings.filter((b) => b.type === type);
  const floors = locations.filter((l) => l.level === 'FLOOR' && String(l.parentId) === String(buildingId));

  // The selected location remains the most specific configured location
  // (floor when selected, otherwise building). Room number is stored separately.
  const selected = floorId || buildingId || '';

  useEffect(() => {
    onChange(selected ? Number(selected) : null);
  }, [selected, onChange]);

  const path = locations.find((l) => String(l.id) === String(selected))?.path;

  return (
    <fieldset className="location-picker">
      <legend>Location</legend>
      <div className="grid-2">
        <label>Location type
          <select value={type} onChange={(e) => { setType(e.target.value); setBuildingId(''); setFloorId(''); }}>
            <option value="">Select type</option>
            {types.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </label>

        <label>Building / Block
          <select value={buildingId} disabled={!type} onChange={(e) => { setBuildingId(e.target.value); setFloorId(''); }}>
            <option value="">Select building</option>
            {typeBuildings.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
          </select>
        </label>

        <label>Floor <span className="muted">(optional)</span>
          <select value={floorId} disabled={!buildingId || floors.length === 0} onChange={(e) => setFloorId(e.target.value)}>
            <option value="">{floors.length === 0 && buildingId ? 'No floors defined' : 'Whole building / any floor'}</option>
            {floors.map((f) => <option key={f.id} value={f.id}>{f.name}</option>)}
          </select>
        </label>

        <label>Room / Area <span className="muted">(optional)</span>
          <input
            type="text"
            value={roomNumber}
            onChange={(e) => onRoomChange?.(e.target.value)}
            maxLength={40}
            placeholder="e.g. 204, Lab 3, 12A"
          />
          {roomError && <p className="field-error">{roomError}</p>}
        </label>
      </div>

      {path && <p className="picked-path">Selected: <strong>{path}</strong></p>}
      {error && <p className="field-error">{error}</p>}
    </fieldset>
  );
}
