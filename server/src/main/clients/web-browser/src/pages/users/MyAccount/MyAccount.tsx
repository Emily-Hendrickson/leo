import './MyAccount.scss';
import {CommonAccountFields} from '../../../libs/CommonAccountFields/CommonAccountFields';
import {DefaultPage} from '../../../libs/DefaultPage/DefaultPage';
import {getCurrentUser, sendToLogin} from '../../../libs/authentication';
import {useEffect, useState} from 'react';
import {pl_types, user_management} from '../../../generated/protobuf-js';
import IUser = pl_types.IUser;
import {Button, Form} from 'antd';
import {createService} from '../../../libs/protos';
import UserManagementService = user_management.UserManagementService;
import IUpsertUserRequest = user_management.IUpsertUserRequest;
import {
  HandleError,
  HandleErrorType,
} from '../../../libs/HandleError/HandleError';

export function MyAccount() {
  const [handleError, setHandleError] = useState<HandleErrorType>();
  const user = getCurrentUser();
  if (user == null) {
    return sendToLogin();
  }

  const [form] = Form.useForm();
  const [userX, setUserX] = useState<IUser | undefined>();
  const [errorMessage, setErrorMessage] = useState('');

  const userService = createService(
    UserManagementService,
    'UserManagementService'
  );

  useEffect(() => {
    form.setFieldsValue(userX ?? {});
  }, [userX]);

  useEffect(() => {
    userService
      .getUserDetails({userXId: user.userXId})
      .then(response => setUserX(response.user?.user ?? {}))
      .catch(setHandleError);
  }, []);

  useEffect(() => {
    if (errorMessage) {
      setTimeout(() => setErrorMessage(''), 5000);
    }
  }, [errorMessage]);

  function finish(values: FormData) {
    const newUserX = Object.assign({}, userX ?? {}, values as IUser);
    const upsertRequest = {user: newUserX, ...values} as IUpsertUserRequest;
    userService
      .upsertUser(upsertRequest)
      .then(response => {
        if (response.error) {
          setErrorMessage(response.error);
        } else {
          setUserX(response.user?.user ?? {});
        }
      })
      .catch(reason => setHandleError({error: reason, reload: false}));
  }

  return (
    <>
      <HandleError error={handleError} setError={setHandleError} />
      <DefaultPage title="My Account">
        <div className="space-filler">
          <div
            className="error-notice"
            style={{display: errorMessage !== '' ? undefined : 'none'}}
          >
            {errorMessage}
          </div>
        </div>
        <div className="form-container">
          <Form form={form} onFinish={finish}>
            <CommonAccountFields form={form} />
            <Form.Item>
              <Button type="primary" htmlType="submit">
                Save
              </Button>
            </Form.Item>
          </Form>
        </div>
      </DefaultPage>
    </>
  );
}
