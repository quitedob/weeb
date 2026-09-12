# Interface color audit

Verification phase: the 56-test result below is the earlier UI/group-contract snapshot. The final release-hardening run passed 159 frontend tests across 28 suites; its reproducible `scripts/color_audit.py` scan covers 59 active files and 454 literal occurrences with zero styling findings. The [final report and manifest](release-verification.md) bind the scan and reviewed browser screenshot to source hash `f4afc1f4fea9c5db99d3621b2552846dfde163bf7bf6204dfc3d8eb8cdf3e0f8`.

The active Vue interface uses blue and neutral accents in place of purple. The audit covered all 59 `.vue` and `.css` files under `Vue`, excluding dependencies, generated `dist` output, local verification files, and archived code.

| Previous styling | Replacement | Active locations |
| --- | --- | --- |
| `--apple-purple` (`#AF52DE`, dark/system-dark `#BF5AF2`) | Removed the three declarations | `src/assets/apple-style.css` |
| Blue/purple and purple/pink gradients | Shared `--apple-accent-gradient` | `AsideMenu`, `Login`, `Register`, `Forget`, `ChatWindow`, two `UserLevelHistory` badges |
| `#667eea` / `#764ba2` gradients | Shared `--apple-accent-gradient` | `ResetPassword`, `SecurityCenter`, `SearchPage` |
| Purple level 5 / level 8 labels | Theme blue / secondary text | `UserLevelHistory` |
| Violet-tinted system grays, including `#F2F2F7`, `#787880`, and `rgba(28, 28, 30, 0.95)` | Neutral grays with equal RGB channels | `src/assets/apple-style.css`, `Login`, `Register`, `SimpleTabs`, `ContactPage` |

The shared gradient runs from the existing blue `#0056CC` to `#0056B3`. Its endpoints provide white-text contrast ratios of 6.56:1 and 7.04:1. These background colors remain the same in light, dark, and system themes so avatar letters and security-card text remain legible. Level labels use existing theme tokens that adapt to dark mode.

The final source scan, repeated after the New Chat dialog update, inspected 454 hex/RGB/HSL literal occurrences (117 distinct values after case normalization), including alpha colors and URL-encoded SVG hex colors. It checked purple/violet/indigo color names and aliases, hues from 225–235 degrees with saturation of at least 35%, and every hue from 235–330 degrees, including faint gray tints. No styling candidates remained. All gradient contexts were also inspected, including variables; the remaining gradients use blue, green, or neutral colors. The purple-heart emoji remains a message-content choice in the emoji catalog, separate from interface styling.

Backend level colors are maintained separately in `src/main/java/com/web/constant/UserLevel.java`; levels 5 and 8 use blue shades `#007AFF` and `#0056B3`.

Validation: all 56 Vue tests passed after the group-detail contract fixes. The final production build passed in 12.83 seconds, including the shared Vite `globalThis` correction and accessible New Chat dialog. Group-detail changes did not alter styles. Light and dark browser previews were reviewed. `git diff HEAD --check` passed. Existing mixed static/dynamic import warnings remain.
