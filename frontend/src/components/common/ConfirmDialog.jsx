/**
 * A modal-style confirmation dialog.
 *
 * @param {object}   props
 * @param {boolean}  props.isOpen    - Whether the dialog is visible
 * @param {string}   props.message   - The confirmation question
 * @param {Function} props.onConfirm - Called when the user confirms
 * @param {Function} props.onCancel  - Called when the user cancels
 */
function ConfirmDialog({ isOpen, message, onConfirm, onCancel }) {
  if (!isOpen) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-dialog-message"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
    >
      <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
        <p
          id="confirm-dialog-message"
          className="mb-6 text-sm text-gray-700"
        >
          {message}
        </p>

        <div className="flex justify-end gap-3">
          <button
            type="button"
            onClick={onCancel}
            className="rounded border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-400"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className="rounded bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500"
          >
            Confirm
          </button>
        </div>
      </div>
    </div>
  );
}

export default ConfirmDialog;
