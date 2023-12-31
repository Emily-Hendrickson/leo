import './MyProjects.scss';
import {useEffect, useRef, useState} from 'react';
import {
  createService,
  pl_types,
  project_management,
} from '../../../libs/protos';
import {DefaultPage} from '../../../libs/DefaultPage/DefaultPage';
import {getCurrentUser, sendToLogin} from '../../../libs/authentication';
import {Dropdown} from 'antd';
import {ProjectPage} from '../../../libs/ProjectPage/ProjectPage';
import IProject = pl_types.IProject;
import ProjectManagementService = project_management.ProjectManagementService;
import {ItemType} from 'antd/es/menu/hooks/useItems';
import IProjectPost = pl_types.IProjectPost;
import {
  HandleError,
  HandleErrorType,
} from '../../../libs/HandleError/HandleError';

export function MyProjects() {
  const [handleError, setHandleError] = useState<HandleErrorType>();
  const user = getCurrentUser();
  if (user == null) {
    return sendToLogin();
  }

  const projectId = useRef<number | undefined>();
  const [projects, setProjects] = useState<IProject[]>([]);
  const [project, setProject] = useState<IProject | undefined>();
  const [posts, setPosts] = useState<IProjectPost[] | undefined>();

  const service = createService(
    ProjectManagementService,
    'ProjectManagementService'
  );

  useEffect(() => {
    service
      .getProjects({userXId: user!.userXId, activeOnly: true})
      .then(response => {
        setProjects(response.projects);
      })
      .catch(setHandleError);
  }, []);

  useEffect(() => {
    if (project == null) {
      projectId.current = undefined;
      setPosts(undefined);
    } else {
      projectId.current = project.id!;
      service
        .getProjectPosts({projectId: project!.id!})
        .then(response => {
          if (project.id! === projectId.current) {
            setPosts(response.projectPosts);
          }
        })
        .catch(reason => setHandleError({error: reason, reload: false}));
    }
  }, [project]);

  function updateProject(project: IProject, modifications: IProject) {
    service.updateProject({id: project.id!, modifications: modifications});

    const newProjects = [...projects];
    Object.assign(project, modifications);
    setProjects(newProjects);
  }

  function postMessage(name: string, messageHtml: string) {
    service
      .postMessage({
        projectId: project!.id!,
        name: name,
        messageHtml: messageHtml,
      })
      .then(response => {
        if (project!.id! === projectId.current) {
          const newPosts: IProjectPost[] = [
            ...(posts ?? []),
            {
              id: response.projectPostId!,
              name: name,
              messageHtml: messageHtml,
              user: user!,
            },
          ];
          setPosts(newPosts);
        }
      })
      .catch(reason => setHandleError({error: reason, reload: false}));
  }

  function deletePost(post: pl_types.IProjectPost) {
    service
      .deletePost({id: post.id!})
      .then(() => {
        if (project!.id! === projectId.current) {
          const newPosts: IProjectPost[] = [...(posts ?? [])].filter(
            p => p.id !== post.id
          );
          setPosts(newPosts);
        }
      })
      .catch(reason => setHandleError({error: reason, reload: false}));
  }

  return (
    <>
      <HandleError error={handleError} setError={setHandleError} />
      <DefaultPage title="My Projects">
        <div style={{width: '100%'}}>
          <Dropdown.Button
            menu={{
              items: projects.map(project => ({
                key: project.id,
                label: (
                  <div
                    className="project-menu"
                    onClick={() => setProject(project)}
                    style={{width: '100%'}}
                  >
                    <span className="name">{project.name!}</span>
                    <span className="descr">{project.shortDescr!}</span>
                  </div>
                ),
              })) as ItemType[],
            }}
            style={{width: '100%'}}
          >
            {project != null ? <>{project.name!}</> : <>Select project...</>}
          </Dropdown.Button>
          {project != null ? (
            <ProjectPage
              id={project.id!}
              key={project.id!}
              name={project.name!}
              shortDescr={project.shortDescr!}
              longDescrHtml={project.longDescrHtml!}
              posts={posts}
              milestones={[]}
              updateProject={modifications =>
                updateProject(project, modifications)
              }
              onDeletePost={post => deletePost(post)}
              onSubmitPost={postMessage}
              editable={true}
            />
          ) : (
            <></>
          )}
        </div>
      </DefaultPage>
    </>
  );
}
