import { describe, it, expect, vi } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import { renderWithProviders } from '@/test/utils';
import { TextEditor } from '@/components/channel-reply/TextEditor';

describe('TextEditor', () => {
  it('should render with initial value', () => {
    const onChange = vi.fn();
    renderWithProviders(<TextEditor value="Hello world" onChange={onChange} />);

    const textarea = screen.getByRole('textbox');
    expect(textarea).toHaveValue('Hello world');
  });

  it('should call onChange when text changes', () => {
    const onChange = vi.fn();
    renderWithProviders(<TextEditor value="" onChange={onChange} />);

    const textarea = screen.getByRole('textbox');
    fireEvent.change(textarea, { target: { value: 'New text' } });

    expect(onChange).toHaveBeenCalledWith('New text');
  });

  it('should show character counter', () => {
    const onChange = vi.fn();
    renderWithProviders(<TextEditor value="Hello" onChange={onChange} maxLength={100} />);

    expect(screen.getByText('5 / 100')).toBeInTheDocument();
  });

  it('should prevent exceeding max length', () => {
    const onChange = vi.fn();
    renderWithProviders(<TextEditor value="" onChange={onChange} maxLength={10} />);

    const textarea = screen.getByRole('textbox');
    const longText = 'This text is way too long';
    fireEvent.change(textarea, { target: { value: longText } });

    // Should truncate to max length
    expect(onChange).toHaveBeenCalledWith(longText.substring(0, 10));
  });

  it('should show red counter when near limit', () => {
    const onChange = vi.fn();
    const maxLength = 100;
    const nearLimitText = 'x'.repeat(95); // 95% of limit

    renderWithProviders(<TextEditor value={nearLimitText} onChange={onChange} maxLength={maxLength} />);

    const counter = screen.getByText(`${nearLimitText.length} / ${maxLength}`);
    expect(counter).toHaveStyle({ color: 'var(--tgui--destructive_text_color)' });
  });

  it('should show normal counter when below limit', () => {
    const onChange = vi.fn();
    const maxLength = 100;
    const text = 'Short text';

    renderWithProviders(<TextEditor value={text} onChange={onChange} maxLength={maxLength} />);

    const counter = screen.getByText(`${text.length} / ${maxLength}`);
    expect(counter).toHaveStyle({ color: 'var(--tgui--hint_color)' });
  });

  it('should handle null value', () => {
    const onChange = vi.fn();
    renderWithProviders(<TextEditor value={null} onChange={onChange} />);

    const textarea = screen.getByRole('textbox');
    expect(textarea).toHaveValue('');
  });

  it('should be disabled when disabled prop is true', () => {
    const onChange = vi.fn();
    renderWithProviders(<TextEditor value="Test" onChange={onChange} disabled />);

    const textarea = screen.getByRole('textbox');
    expect(textarea).toBeDisabled();
  });

  it('should call onChange with null for empty string', () => {
    const onChange = vi.fn();
    renderWithProviders(<TextEditor value="text" onChange={onChange} />);

    const textarea = screen.getByRole('textbox');
    fireEvent.change(textarea, { target: { value: '' } });

    expect(onChange).toHaveBeenCalledWith(null);
  });
});
