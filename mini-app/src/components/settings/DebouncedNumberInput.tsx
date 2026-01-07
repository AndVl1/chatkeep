import { useState, useEffect, useCallback } from 'react';
import { Input } from '@telegram-apps/telegram-ui';
import { useDebouncedValue } from '@/hooks/ui/useDebouncedValue';

interface DebouncedNumberInputProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  disabled?: boolean;
  placeholder?: string;
}

/**
 * Number input that debounces onChange calls.
 * Allows users to clear the field while typing without triggering validation errors.
 * Only calls onChange with valid numbers after the user stops typing.
 */
export function DebouncedNumberInput({
  value,
  onChange,
  min,
  max,
  disabled,
  placeholder,
}: DebouncedNumberInputProps) {
  // Local state for the input (string to allow empty/partial values)
  const [localValue, setLocalValue] = useState<string>(String(value));

  // Debounced version of the local value
  const debouncedValue = useDebouncedValue(localValue, 500);

  // Sync local state when props.value changes externally
  useEffect(() => {
    setLocalValue(String(value));
  }, [value]);

  // Handle debounced value changes
  useEffect(() => {
    // Don't process if input is empty or same as current value
    if (debouncedValue === '' || debouncedValue === String(value)) {
      return;
    }

    const parsed = parseInt(debouncedValue, 10);

    // Only call onChange if we have a valid number
    if (!isNaN(parsed)) {
      // Apply min/max constraints
      let validValue = parsed;
      if (min !== undefined) {
        validValue = Math.max(min, validValue);
      }
      if (max !== undefined) {
        validValue = Math.min(max, validValue);
      }

      // Only call onChange if value actually changed
      if (validValue !== value) {
        onChange(validValue);
      }
    }
  }, [debouncedValue, value, onChange, min, max]);

  const handleChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;

    // Allow empty string (user is clearing/typing)
    if (newValue === '') {
      setLocalValue('');
      return;
    }

    // Allow only valid number strings (including negative sign if needed)
    if (/^-?\d*$/.test(newValue)) {
      setLocalValue(newValue);
    }
  }, []);

  return (
    <Input
      type="text"
      inputMode="numeric"
      value={localValue}
      onChange={handleChange}
      disabled={disabled}
      placeholder={placeholder}
    />
  );
}
