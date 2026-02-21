import React from 'react';
import UserRegisterForm from './UserRegisterForm';
import { useEscapeKey } from '../../hooks/useKeyboardShortcuts';

/**
 * ModalEmployeesRegister component.
 * A modal wrapper specifically designed for employee and admin registration.
 * 
 * Props:
 *  - onCreate: async callback function triggered on successful form submission.
 *  - onCancel: callback function triggered when the modal is closed.
 *  - isSuper: boolean indicating if the current user is a superadmin.
 *  - isAdmin: boolean indicating if the current user is an admin.
 *  - defaultRole: the default role selected in the form.
 *  - title: the title of the modal.
 *  - allowedRoles: array of roles available for selection.
 *  - hideRoleField: boolean to hide the role selection field.
 */
const ModalEmployeesRegister = ({ onCreate, onCancel, isSuper = false, isAdmin = false, defaultRole = 'EMPLOYEE', title = 'Añadir empleado', allowedRoles = null, hideRoleField = false }) => {
  // Close modal with Escape key (Nielsen Heuristic #3: User Control and Freedom)
  useEscapeKey(onCancel);

  return (
    <div className="tool-overlay" onClick={onCancel}>
      <div className="tool-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '560px', width: '90%', padding: '28px 30px' }}>
        <button className="close-btn" onClick={onCancel}>✕</button>

        <UserRegisterForm
          isModal={true}
          initial={{ rol: defaultRole }}
          requirePassword={true}
          isSuper={isSuper}
          allowedRoles={allowedRoles}
          defaultRole={defaultRole}
          hideRoleField={hideRoleField}
          title={title}
          submitLabel={title}
          onSubmit={async (form) => {
            if (onCreate) await onCreate(form);
          }}
          onCancel={onCancel}
        />
      </div>
    </div>
  );
};

export default ModalEmployeesRegister;
