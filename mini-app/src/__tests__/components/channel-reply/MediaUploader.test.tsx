import { describe, it, expect, vi } from 'vitest';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@/test/utils';
import { MediaUploader } from '@/components/channel-reply/MediaUploader';

describe('MediaUploader', () => {
  it('should render upload button', () => {
    const onUpload = vi.fn();
    renderWithProviders(<MediaUploader onUpload={onUpload} />);

    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  it('should trigger file input on button click', () => {
    const onUpload = vi.fn();
    renderWithProviders(<MediaUploader onUpload={onUpload} />);

    const button = screen.getByRole('button');
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;

    const clickSpy = vi.spyOn(fileInput, 'click');

    fireEvent.click(button);

    expect(clickSpy).toHaveBeenCalled();
  });

  it('should validate file size (reject > 20MB)', async () => {
    const onUpload = vi.fn();
    renderWithProviders(<MediaUploader onUpload={onUpload} />);

    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;

    // Create a mock file larger than 20MB
    const largeFile = new File(['x'.repeat(21 * 1024 * 1024)], 'large.jpg', {
      type: 'image/jpeg',
    });

    Object.defineProperty(fileInput, 'files', {
      value: [largeFile],
      writable: false,
    });

    fireEvent.change(fileInput);

    await waitFor(() => {
      expect(onUpload).not.toHaveBeenCalled();
    });
  });

  it('should validate MIME type (reject invalid types)', async () => {
    const onUpload = vi.fn();
    renderWithProviders(<MediaUploader onUpload={onUpload} />);

    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;

    // Create a file with invalid MIME type
    const invalidFile = new File(['content'], 'document.pdf', {
      type: 'application/pdf',
    });

    Object.defineProperty(fileInput, 'files', {
      value: [invalidFile],
      writable: false,
    });

    fireEvent.change(fileInput);

    await waitFor(() => {
      expect(onUpload).not.toHaveBeenCalled();
    });
  });

  it('should accept valid image file', async () => {
    const onUpload = vi.fn().mockResolvedValue(undefined);
    renderWithProviders(<MediaUploader onUpload={onUpload} />);

    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;

    const validFile = new File(['image content'], 'photo.jpg', {
      type: 'image/jpeg',
    });

    Object.defineProperty(fileInput, 'files', {
      value: [validFile],
      writable: false,
    });

    fireEvent.change(fileInput);

    await waitFor(() => {
      expect(onUpload).toHaveBeenCalledWith(validFile);
    });
  });

  it('should accept valid video file', async () => {
    const onUpload = vi.fn().mockResolvedValue(undefined);
    renderWithProviders(<MediaUploader onUpload={onUpload} />);

    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;

    const validFile = new File(['video content'], 'video.mp4', {
      type: 'video/mp4',
    });

    Object.defineProperty(fileInput, 'files', {
      value: [validFile],
      writable: false,
    });

    fireEvent.change(fileInput);

    await waitFor(() => {
      expect(onUpload).toHaveBeenCalledWith(validFile);
    });
  });

  it('should show uploading state', () => {
    const onUpload = vi.fn();
    renderWithProviders(<MediaUploader onUpload={onUpload} isUploading />);

    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
  });

  it('should be disabled when disabled prop is true', () => {
    const onUpload = vi.fn();
    renderWithProviders(<MediaUploader onUpload={onUpload} disabled />);

    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
  });

  it('should clear file input after successful upload', async () => {
    const onUpload = vi.fn().mockResolvedValue(undefined);
    renderWithProviders(<MediaUploader onUpload={onUpload} />);

    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;

    const validFile = new File(['content'], 'photo.jpg', {
      type: 'image/jpeg',
    });

    Object.defineProperty(fileInput, 'files', {
      value: [validFile],
      writable: false,
    });

    const valueSetter = vi.spyOn(fileInput, 'value', 'set');

    fireEvent.change(fileInput);

    await waitFor(() => {
      expect(valueSetter).toHaveBeenCalledWith('');
    });
  });

  it('should clear file input after failed upload', async () => {
    const onUpload = vi.fn().mockRejectedValue(new Error('Upload failed'));
    renderWithProviders(<MediaUploader onUpload={onUpload} />);

    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;

    const validFile = new File(['content'], 'photo.jpg', {
      type: 'image/jpeg',
    });

    Object.defineProperty(fileInput, 'files', {
      value: [validFile],
      writable: false,
    });

    const valueSetter = vi.spyOn(fileInput, 'value', 'set');

    fireEvent.change(fileInput);

    await waitFor(() => {
      expect(valueSetter).toHaveBeenCalledWith('');
    });
  });
});
