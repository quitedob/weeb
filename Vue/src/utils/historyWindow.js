// Offset pages can move when new records arrive. Locate the viewed boundary by
// identity before joining adjacent rows; never join two unrelated page snapshots.
export async function adjacentHistoryPage({ anchorId, direction, pageHint, size, existingIds, readPage, isCurrent }) {
  const key = String(anchorId);
  let page = Math.max(1, pageHint);
  let trail = [];
  let adjacent = [];
  let anchorOffset;
  const seen = new Set(existingIds.map(String));
  let metadata;
  // A stale or removed anchor must not cause an endless background traversal.
  // Keep the current window intact on failure; latest remains independently reloadable.
  for (let reads = 0; reads < 40; reads++) {
    metadata = await readPage(page, size);
    if (!isCurrent()) return null;
    const rows = metadata.rows;
    const end = rows.length < size || (metadata.totalPages != null && page >= metadata.totalPages);
    const index = rows.findIndex(row => String(row.id) === key);
    if (anchorOffset == null) {
      if (index < 0) {
        if (end) throw new Error('History changed; return to the latest records and retry');
        if (direction === 'newer') trail = uniqueRows([...trail, ...rows]).slice(-size);
        ++page;
        continue;
      }
      anchorOffset = (page - 1) * size + index;
      if (direction === 'newer') {
        adjacent = uniqueRows([...trail, ...rows.slice(0, index)]).slice(-size);
        return { rows: adjacent, anchorOffset, hasMore: true,
          firstOffset: anchorOffset - adjacent.length, metadata };
      }
    }
    const candidates = index >= 0 ? rows.slice(index + 1) : rows;
    for (const row of candidates) {
      if (!seen.has(String(row.id))) { seen.add(String(row.id)); adjacent.push(row); }
    }
    if (adjacent.length >= size || end) {
      return { rows: adjacent.slice(0, size), anchorOffset,
        lastOffset: anchorOffset + Math.min(adjacent.length, size),
        hasMore: adjacent.length > size || !end, metadata };
    }
    ++page;
  }
  throw new Error('History moved beyond this window; return to the latest records and retry');
}

function uniqueRows(rows) {
  const ids = new Set();
  return rows.filter(row => { const key = String(row.id); if (ids.has(key)) return false; ids.add(key); return true; });
}
