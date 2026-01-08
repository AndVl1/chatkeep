import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { AppRoot } from '@telegram-apps/telegram-ui';
import { SettingsForm } from '@/components/settings/SettingsForm';
import type { ChatSettings } from '@/types';

const mockSettings: ChatSettings = {
  chatId: 123,
  chatTitle: 'Test Chat',
  collectionEnabled: true,
  cleanServiceEnabled: false,
  maxWarnings: 3,
  warningTtlHours: 24,
  thresholdAction: 'WARN',
  thresholdDurationMinutes: null,
  defaultBlocklistAction: 'NOTHING',
  logChannelId: null,
  lockWarnsEnabled: false,
};

function renderWithAppRoot(component: React.ReactElement) {
  return render(<AppRoot>{component}</AppRoot>);
}

describe('SettingsForm', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('Toggle switches', () => {
    it('should call onChange with collectionEnabled: false when toggle is switched off', () => {
      const onChange = vi.fn();
      renderWithAppRoot(<SettingsForm settings={mockSettings} onChange={onChange} />);

      const collectionSwitch = screen.getByRole('checkbox', { name: /collection enabled/i });
      expect(collectionSwitch).toBeChecked();

      fireEvent.click(collectionSwitch);
      expect(onChange).toHaveBeenCalledWith({ collectionEnabled: false });
    });

    it('should call onChange with collectionEnabled: true when toggle is switched on', () => {
      const onChange = vi.fn();
      const settings = { ...mockSettings, collectionEnabled: false };
      renderWithAppRoot(<SettingsForm settings={settings} onChange={onChange} />);

      const collectionSwitch = screen.getByRole('checkbox', { name: /collection enabled/i });
      expect(collectionSwitch).not.toBeChecked();

      fireEvent.click(collectionSwitch);
      expect(onChange).toHaveBeenCalledWith({ collectionEnabled: true });
    });

    it('should call onChange with cleanServiceEnabled: true when toggle is switched on', () => {
      const onChange = vi.fn();
      renderWithAppRoot(<SettingsForm settings={mockSettings} onChange={onChange} />);

      const cleanServiceSwitch = screen.getByRole('checkbox', { name: /clean service messages/i });
      expect(cleanServiceSwitch).not.toBeChecked();

      fireEvent.click(cleanServiceSwitch);
      expect(onChange).toHaveBeenCalledWith({ cleanServiceEnabled: true });
    });

    it('should call onChange with cleanServiceEnabled: false when toggle is switched off', () => {
      const onChange = vi.fn();
      const settings = { ...mockSettings, cleanServiceEnabled: true };
      renderWithAppRoot(<SettingsForm settings={settings} onChange={onChange} />);

      const cleanServiceSwitch = screen.getByRole('checkbox', { name: /clean service messages/i });
      expect(cleanServiceSwitch).toBeChecked();

      fireEvent.click(cleanServiceSwitch);
      expect(onChange).toHaveBeenCalledWith({ cleanServiceEnabled: false });
    });

    it('should call onChange with lockWarnsEnabled: true when toggle is switched on', () => {
      const onChange = vi.fn();
      renderWithAppRoot(<SettingsForm settings={mockSettings} onChange={onChange} />);

      const lockWarnsSwitch = screen.getByRole('checkbox', { name: /violation warnings/i });
      expect(lockWarnsSwitch).not.toBeChecked();

      fireEvent.click(lockWarnsSwitch);
      expect(onChange).toHaveBeenCalledWith({ lockWarnsEnabled: true });
    });

    it('should call onChange with lockWarnsEnabled: false when toggle is switched off', () => {
      const onChange = vi.fn();
      const settings = { ...mockSettings, lockWarnsEnabled: true };
      renderWithAppRoot(<SettingsForm settings={settings} onChange={onChange} />);

      const lockWarnsSwitch = screen.getByRole('checkbox', { name: /violation warnings/i });
      expect(lockWarnsSwitch).toBeChecked();

      fireEvent.click(lockWarnsSwitch);
      expect(onChange).toHaveBeenCalledWith({ lockWarnsEnabled: false });
    });
  });

  describe('Rendering', () => {
    it('should render all three toggle switches', () => {
      const onChange = vi.fn();
      renderWithAppRoot(<SettingsForm settings={mockSettings} onChange={onChange} />);

      expect(screen.getByRole('checkbox', { name: /collection enabled/i })).toBeInTheDocument();
      expect(screen.getByRole('checkbox', { name: /clean service messages/i })).toBeInTheDocument();
      expect(screen.getByRole('checkbox', { name: /violation warnings/i })).toBeInTheDocument();
    });

    it('should render correct initial states for toggles', () => {
      const onChange = vi.fn();
      const settings: ChatSettings = {
        ...mockSettings,
        collectionEnabled: true,
        cleanServiceEnabled: true,
        lockWarnsEnabled: false,
      };
      renderWithAppRoot(<SettingsForm settings={settings} onChange={onChange} />);

      expect(screen.getByRole('checkbox', { name: /collection enabled/i })).toBeChecked();
      expect(screen.getByRole('checkbox', { name: /clean service messages/i })).toBeChecked();
      expect(screen.getByRole('checkbox', { name: /violation warnings/i })).not.toBeChecked();
    });

    it('should disable all inputs when disabled prop is true', () => {
      const onChange = vi.fn();
      renderWithAppRoot(<SettingsForm settings={mockSettings} onChange={onChange} disabled />);

      expect(screen.getByRole('checkbox', { name: /collection enabled/i })).toBeDisabled();
      expect(screen.getByRole('checkbox', { name: /clean service messages/i })).toBeDisabled();
      expect(screen.getByRole('checkbox', { name: /violation warnings/i })).toBeDisabled();
    });
  });

  describe('Number inputs', () => {
    it('should call onChange with maxWarnings when input changes', async () => {
      const onChange = vi.fn();
      renderWithAppRoot(<SettingsForm settings={mockSettings} onChange={onChange} />);

      const maxWarningsInput = screen.getByDisplayValue('3');

      act(() => {
        fireEvent.change(maxWarningsInput, { target: { value: '5' } });
      });

      // Advance timers to trigger debounce
      await act(async () => {
        vi.advanceTimersByTime(500);
      });

      expect(onChange).toHaveBeenCalledWith({ maxWarnings: 5 });
    });

    it('should call onChange with warningTtlHours when input changes', async () => {
      const onChange = vi.fn();
      renderWithAppRoot(<SettingsForm settings={mockSettings} onChange={onChange} />);

      const warningTtlInput = screen.getByDisplayValue('24');

      act(() => {
        fireEvent.change(warningTtlInput, { target: { value: '48' } });
      });

      // Advance timers to trigger debounce
      await act(async () => {
        vi.advanceTimersByTime(500);
      });

      expect(onChange).toHaveBeenCalledWith({ warningTtlHours: 48 });
    });
  });

  describe('Select inputs', () => {
    it('should call onChange with thresholdAction when select changes', () => {
      const onChange = vi.fn();
      renderWithAppRoot(<SettingsForm settings={mockSettings} onChange={onChange} />);

      const thresholdActionSelect = screen.getByDisplayValue('Warn');
      fireEvent.change(thresholdActionSelect, { target: { value: 'BAN' } });

      expect(onChange).toHaveBeenCalledWith({ thresholdAction: 'BAN' });
    });

    it('should call onChange with defaultBlocklistAction when select changes', () => {
      const onChange = vi.fn();
      renderWithAppRoot(<SettingsForm settings={mockSettings} onChange={onChange} />);

      const blocklistActionSelect = screen.getByDisplayValue('Do nothing');
      fireEvent.change(blocklistActionSelect, { target: { value: 'KICK' } });

      expect(onChange).toHaveBeenCalledWith({ defaultBlocklistAction: 'KICK' });
    });
  });
});
