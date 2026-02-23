import { Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Label } from "@/components/ui/label";

export type TaskVariableType =
  | "String"
  | "Integer"
  | "Long"
  | "Double"
  | "Float"
  | "Boolean"
  | "Json";

export interface TaskVariableRow {
  id: string;
  name: string;
  type: TaskVariableType;
  value: string;
}

const VARIABLE_TYPES: TaskVariableType[] = [
  "String",
  "Integer",
  "Long",
  "Double",
  "Float",
  "Boolean",
  "Json",
];

interface TaskVariableEditorProps {
  rows: TaskVariableRow[];
  onChange: (rows: TaskVariableRow[]) => void;
  errors?: Record<string, string>;
}

export function TaskVariableEditor({
  rows,
  onChange,
  errors = {},
}: TaskVariableEditorProps) {
  const addRow = () => {
    onChange([
      ...rows,
      {
        id: crypto.randomUUID(),
        name: "",
        type: "String",
        value: "",
      },
    ]);
  };

  const updateRow = (id: string, patch: Partial<TaskVariableRow>) => {
    onChange(rows.map((row) => (row.id === id ? { ...row, ...patch } : row)));
  };

  const removeRow = (id: string) => {
    onChange(rows.filter((row) => row.id !== id));
  };

  return (
    <div className="space-y-4">
      <div className="hidden md:grid grid-cols-[2fr_1fr_2fr_44px] gap-2 px-1">
        <Label className="text-xs text-muted-foreground">Name</Label>
        <Label className="text-xs text-muted-foreground">Type</Label>
        <Label className="text-xs text-muted-foreground">Value</Label>
      </div>
      <div className="space-y-3">
        {rows.length === 0 && (
          <div className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
            No variables added yet.
          </div>
        )}
        {rows.map((row) => (
          <div
            key={row.id}
            className="rounded-lg border border-border/70 bg-muted/20 p-3"
          >
            <div className="grid gap-2 md:grid-cols-[2fr_1fr_2fr_44px]">
              <div>
                <Label className="text-xs text-muted-foreground md:hidden">
                  Name
                </Label>
                <Input
                  placeholder="variableName"
                  value={row.name}
                  onChange={(event) =>
                    updateRow(row.id, { name: event.target.value })
                  }
                  className={errors[row.id] ? "border-destructive" : ""}
                />
                {errors[row.id] && (
                  <p className="mt-1 text-xs text-destructive">{errors[row.id]}</p>
                )}
              </div>
              <div>
                <Label className="text-xs text-muted-foreground md:hidden">
                  Type
                </Label>
                <Select
                  value={row.type}
                  onValueChange={(value) =>
                    updateRow(row.id, {
                      type: value as TaskVariableType,
                      value:
                        value === "Boolean"
                          ? "false"
                          : value === "Json"
                            ? "{}"
                            : "",
                    })
                  }
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {VARIABLE_TYPES.map((type) => (
                      <SelectItem key={type} value={type}>
                        {type}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground md:hidden">
                  Value
                </Label>
                {row.type === "Boolean" ? (
                  <Select
                    value={row.value}
                    onValueChange={(value) => updateRow(row.id, { value })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="true">true</SelectItem>
                      <SelectItem value="false">false</SelectItem>
                    </SelectContent>
                  </Select>
                ) : (
                  <Input
                    value={row.value}
                    placeholder={row.type === "Json" ? '{"foo":"bar"}' : "value"}
                    onChange={(event) =>
                      updateRow(row.id, { value: event.target.value })
                    }
                    className="font-mono"
                  />
                )}
              </div>
              <div className="flex items-end md:items-center">
                <Button
                  variant="ghost"
                  size="icon"
                  className="text-muted-foreground hover:text-destructive"
                  onClick={() => removeRow(row.id)}
                  aria-label="Delete variable"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        ))}
      </div>
      <Button variant="outline" onClick={addRow}>
        <Plus className="mr-2 h-4 w-4" />
        Add Variable
      </Button>
    </div>
  );
}
