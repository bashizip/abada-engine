import React, { useState, useEffect } from 'react';
import { Modal, Button } from './ui/Common.tsx';
import { Variable } from '../types';
import { Trash2, Plus } from 'lucide-react';

interface DataSurgeryModalProps {
  isOpen: boolean;
  onClose: () => void;
  variables: Variable[];
  onSave: (newVariables: Variable[]) => void;
}

export const DataSurgeryModal: React.FC<DataSurgeryModalProps> = ({ isOpen, onClose, variables, onSave }) => {
  const [localVars, setLocalVars] = useState<Variable[]>([]);
  const [newVars, setNewVars] = useState<Variable[]>([]);
  const [confirming, setConfirming] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isOpen) {
      setLocalVars(JSON.parse(JSON.stringify(variables))); // Deep copy
      setNewVars([]);
      setConfirming(false);
      setErrors({});
    }
  }, [isOpen, variables]);

  const handleValueChange = (index: number, newValue: string, isNew: boolean) => {
    const vars = isNew ? [...newVars] : [...localVars];
    const v = vars[index];

    // Type conversion logic
    if (v.type === 'Integer' || v.type === 'Long') {
      v.value = parseInt(newValue) || 0;
    } else if (v.type === 'Double' || v.type === 'Float') {
      v.value = parseFloat(newValue) || 0.0;
    } else if (v.type === 'Boolean') {
      v.value = newValue === 'true';
    } else {
      v.value = newValue;
    }

    if (isNew) {
      setNewVars(vars);
    } else {
      setLocalVars(vars);
    }
  };

  const handleNameChange = (index: number, newName: string) => {
    const vars = [...newVars];
    vars[index].name = newName;
    setNewVars(vars);

    // Clear error for this field if it exists
    const errorKey = `new-${index}-name`;
    if (errors[errorKey]) {
      const newErrors = { ...errors };
      delete newErrors[errorKey];
      setErrors(newErrors);
    }
  };

  const handleTypeChange = (index: number, newType: Variable['type']) => {
    const vars = [...newVars];
    vars[index].type = newType;

    // Set default value based on type
    if (newType === 'Boolean') {
      vars[index].value = false;
    } else if (newType === 'Integer' || newType === 'Long') {
      vars[index].value = 0;
    } else if (newType === 'Double' || newType === 'Float') {
      vars[index].value = 0.0;
    } else {
      vars[index].value = '';
    }

    setNewVars(vars);
  };

  const handleAddVariable = () => {
    const newVar: Variable = {
      name: '',
      type: 'String',
      value: ''
    };
    setNewVars([...newVars, newVar]);
  };

  const handleDeleteVariable = (index: number, isNew: boolean) => {
    if (isNew) {
      setNewVars(newVars.filter((_, i) => i !== index));
    } else {
      setLocalVars(localVars.filter((_, i) => i !== index));
    }
  };

  const validateVariables = (): boolean => {
    const newErrors: Record<string, string> = {};
    const allNames = new Set<string>();

    // Check existing variables
    localVars.forEach(v => allNames.add(v.name));

    // Validate new variables
    newVars.forEach((v, idx) => {
      const errorKey = `new-${idx}-name`;

      // Check if name is empty
      if (!v.name.trim()) {
        newErrors[errorKey] = 'Name is required';
        return;
      }

      // Check if name is valid (alphanumeric and underscores)
      if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(v.name)) {
        newErrors[errorKey] = 'Name must start with letter/underscore and contain only alphanumeric/underscores';
        return;
      }

      // Check for duplicates
      if (allNames.has(v.name)) {
        newErrors[errorKey] = 'Duplicate variable name';
        return;
      }

      allNames.add(v.name);
    });

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSave = () => {
    if (!confirming) {
      if (!validateVariables()) {
        return;
      }
      setConfirming(true);
      return;
    }

    // Combine existing and new variables
    const allVariables = [...localVars, ...newVars];
    onSave(allVariables);
    onClose();
  };

  const allVariables = [...localVars, ...newVars];

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={confirming ? "Confirm Data Surgery" : "Edit Process Variables"}
      maxWidth="max-w-4xl"
      footer={
        <div className="flex justify-between w-full">
          {confirming ? (
            <div className="flex items-center gap-4 w-full">
              <div className="text-amber-400 text-sm flex-1">
                Warning: Modifying variables directly can lead to unexpected process behavior. Are you sure?
              </div>
              <Button variant="outline" onClick={() => setConfirming(false)}>Back</Button>
              <Button variant="danger" onClick={handleSave}>Confirm & Apply</Button>
            </div>
          ) : (
            <>
              <Button variant="ghost" onClick={onClose}>Cancel</Button>
              <Button onClick={handleSave}>Review Changes</Button>
            </>
          )}
        </div>
      }
    >
      {!confirming ? (
        <div className="space-y-4">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left text-slate-400">
              <thead className="text-xs uppercase bg-slate-900 text-slate-300">
                <tr>
                  <th className="px-4 py-3 rounded-tl-lg">Variable Name</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Value</th>
                  <th className="px-4 py-3 rounded-tr-lg w-16">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700">
                {/* Existing variables */}
                {localVars.map((v, idx) => (
                  <tr key={`existing-${idx}`} className="bg-slate-800/50 hover:bg-slate-800">
                    <td className="px-4 py-3 font-medium text-slate-200">{v.name}</td>
                    <td className="px-4 py-3 font-mono text-xs text-slate-400">{v.type}</td>
                    <td className="px-4 py-2">
                      {v.type === 'Boolean' ? (
                        <select
                          className="bg-slate-900 border border-slate-600 rounded px-2 py-1 w-full text-slate-200"
                          value={String(v.value)}
                          onChange={(e) => handleValueChange(idx, e.target.value, false)}
                        >
                          <option value="true">true</option>
                          <option value="false">false</option>
                        </select>
                      ) : (
                        <input
                          type="text"
                          className="bg-slate-900 border border-slate-600 rounded px-2 py-1 w-full text-slate-200 font-mono"
                          value={String(v.value)}
                          onChange={(e) => handleValueChange(idx, e.target.value, false)}
                        />
                      )}
                    </td>
                    <td className="px-4 py-2 text-center">
                      <button
                        onClick={() => handleDeleteVariable(idx, false)}
                        className="text-red-400 hover:text-red-300 transition-colors"
                        title="Delete variable"
                      >
                        <Trash2 size={16} />
                      </button>
                    </td>
                  </tr>
                ))}

                {/* New variables */}
                {newVars.map((v, idx) => (
                  <tr key={`new-${idx}`} className="bg-blue-900/20 hover:bg-blue-900/30 border-l-2 border-blue-500">
                    <td className="px-4 py-2">
                      <input
                        type="text"
                        className={`bg-slate-900 border rounded px-2 py-1 w-full text-slate-200 font-mono ${errors[`new-${idx}-name`] ? 'border-red-500' : 'border-slate-600'
                          }`}
                        placeholder="variableName"
                        value={v.name}
                        onChange={(e) => handleNameChange(idx, e.target.value)}
                      />
                      {errors[`new-${idx}-name`] && (
                        <div className="text-red-400 text-xs mt-1">{errors[`new-${idx}-name`]}</div>
                      )}
                    </td>
                    <td className="px-4 py-2">
                      <select
                        className="bg-slate-900 border border-slate-600 rounded px-2 py-1 w-full text-slate-200 text-xs"
                        value={v.type}
                        onChange={(e) => handleTypeChange(idx, e.target.value as Variable['type'])}
                      >
                        <option value="String">String</option>
                        <option value="Integer">Integer</option>
                        <option value="Long">Long</option>
                        <option value="Double">Double</option>
                        <option value="Float">Float</option>
                        <option value="Boolean">Boolean</option>
                      </select>
                    </td>
                    <td className="px-4 py-2">
                      {v.type === 'Boolean' ? (
                        <select
                          className="bg-slate-900 border border-slate-600 rounded px-2 py-1 w-full text-slate-200"
                          value={String(v.value)}
                          onChange={(e) => handleValueChange(idx, e.target.value, true)}
                        >
                          <option value="true">true</option>
                          <option value="false">false</option>
                        </select>
                      ) : (
                        <input
                          type="text"
                          className="bg-slate-900 border border-slate-600 rounded px-2 py-1 w-full text-slate-200 font-mono"
                          placeholder="value"
                          value={String(v.value)}
                          onChange={(e) => handleValueChange(idx, e.target.value, true)}
                        />
                      )}
                    </td>
                    <td className="px-4 py-2 text-center">
                      <button
                        onClick={() => handleDeleteVariable(idx, true)}
                        className="text-red-400 hover:text-red-300 transition-colors"
                        title="Delete variable"
                      >
                        <Trash2 size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="flex justify-start">
            <Button variant="outline" onClick={handleAddVariable} size="sm">
              <Plus size={16} className="mr-2" />
              Add Variable
            </Button>
          </div>
        </div>
      ) : (
        <div className="space-y-2">
          <p className="text-sm text-slate-300">The following changes will be applied to the running process instance:</p>
          <div className="bg-slate-950 p-4 rounded-md font-mono text-xs text-blue-300 max-h-64 overflow-auto">
            {JSON.stringify(allVariables, null, 2)}
          </div>
        </div>
      )}
    </Modal>
  );
};
