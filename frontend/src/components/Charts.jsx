import { Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { BRAND, PRIORITY_COLORS, SERIES, STATUS_COLORS } from '../utils/colors';
import { statusLabel } from '../utils/format';

function Empty() { return <p className="muted chart-empty">No data for the selected filters.</p>; }

/** Horizontal bar chart of { name, count } rows. */
export function CountBars({ data, colorMap, height = 280 }) {
  const rows = (data || []).filter((d) => d.count > 0);
  if (!rows.length) return <Empty />;
  const h = Math.max(height, rows.length * 34 + 40);
  return (
    <ResponsiveContainer width="100%" height={h}>
      <BarChart data={rows} layout="vertical" margin={{ left: 8, right: 16, top: 4, bottom: 4 }}>
        <CartesianGrid strokeDasharray="3 3" horizontal={false} />
        <XAxis type="number" allowDecimals={false} />
        <YAxis type="category" dataKey="name" width={130} tick={{ fontSize: 12 }} tickFormatter={(v) => (colorMap ? statusLabel(v) : v)} />
        <Tooltip formatter={(v) => [v, 'Issues']} labelFormatter={(v) => (colorMap ? statusLabel(v) : v)} />
        <Bar dataKey="count" radius={[0, 3, 3, 0]} fill={BRAND.maroon}>
          {rows.map((r, i) => <Cell key={r.name} fill={colorMap ? colorMap[r.name] || BRAND.maroon : SERIES[i % SERIES.length]} />)}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}

export function StatusPie({ data, height = 280 }) {
  const rows = (data || []).filter((d) => d.count > 0);
  if (!rows.length) return <Empty />;
  return (
    <ResponsiveContainer width="100%" height={height}>
      <PieChart>
        <Pie data={rows} dataKey="count" nameKey="name" innerRadius={55} outerRadius={95} paddingAngle={2}>
          {rows.map((r) => <Cell key={r.name} fill={STATUS_COLORS[r.name] || BRAND.maroon} />)}
        </Pie>
        <Tooltip formatter={(v, n) => [v, statusLabel(n)]} />
        <Legend formatter={(v) => statusLabel(v)} />
      </PieChart>
    </ResponsiveContainer>
  );
}

export function PriorityBars({ data, height = 240 }) {
  return <CountBars data={data} colorMap={PRIORITY_COLORS} height={height} />;
}
export function StatusBars({ data, height = 280 }) {
  return <CountBars data={data} colorMap={STATUS_COLORS} height={height} />;
}

export function TrendLine({ data, height = 260 }) {
  if (!data || !data.length) return <Empty />;
  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={data} margin={{ left: 0, right: 16, top: 8, bottom: 4 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="date" tick={{ fontSize: 11 }} minTickGap={24} />
        <YAxis allowDecimals={false} width={32} />
        <Tooltip formatter={(v) => [v, 'Issues reported']} />
        <Line type="monotone" dataKey="count" stroke={BRAND.maroon} strokeWidth={2} dot={data.length < 45} />
      </LineChart>
    </ResponsiveContainer>
  );
}
