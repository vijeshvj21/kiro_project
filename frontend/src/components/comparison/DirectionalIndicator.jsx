/**
 * Displays a directional indicator (UP / DOWN / NEUTRAL) based on the
 * comparison between specifiedTotal and precedingTotal.
 *
 * Direction logic:
 *   specifiedTotal > precedingTotal  → UP      (↑ green)
 *   specifiedTotal < precedingTotal  → DOWN     (↓ red)
 *   specifiedTotal === precedingTotal → NEUTRAL  (→ grey)
 *
 * @param {object} props
 * @param {number|string} props.specifiedTotal  - Total for the specified period
 * @param {number|string} props.precedingTotal  - Total for the preceding period
 */
function DirectionalIndicator({ specifiedTotal, precedingTotal }) {
  const specified = parseFloat(specifiedTotal);
  const preceding = parseFloat(precedingTotal);

  let direction;
  if (specified > preceding) {
    direction = "UP";
  } else if (specified < preceding) {
    direction = "DOWN";
  } else {
    direction = "NEUTRAL";
  }

  const config = {
    UP: {
      symbol: "↑",
      label: "Increased",
      className: "text-green-600",
      ariaLabel: "Increased",
    },
    DOWN: {
      symbol: "↓",
      label: "Decreased",
      className: "text-red-600",
      ariaLabel: "Decreased",
    },
    NEUTRAL: {
      symbol: "→",
      label: "No change",
      className: "text-gray-500",
      ariaLabel: "No change",
    },
  };

  const { symbol, label, className, ariaLabel } = config[direction];

  return (
    <span
      className={`inline-flex items-center gap-1 font-semibold ${className}`}
      aria-label={ariaLabel}
    >
      <span aria-hidden="true">{symbol}</span>
      <span>{label}</span>
    </span>
  );
}

export default DirectionalIndicator;
